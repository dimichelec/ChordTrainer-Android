package com.slappyapps.chordtrainer

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider



class MainActivity : AppCompatActivity() {

    private lateinit var ivDiagram: ImageView
    private lateinit var cdDiagram: ChordDiagram
    private lateinit var metronome: Metronome
    private var time = 0

    private lateinit var clockHandler: Handler


    interface GestureInterface {
        fun setOnScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float)
        fun onClick(e:MotionEvent)
    }

    class MyGestureDetector(private val gestureInterfacePar: GestureInterface) : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            gestureInterfacePar.onClick(e)
            return false
        }

        override fun onLongPress(e: MotionEvent) {}
        override fun onDoubleTap(e: MotionEvent): Boolean { return false }
        override fun onDoubleTapEvent(e: MotionEvent): Boolean { return false }
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean { return false }
        override fun onShowPress(e: MotionEvent) {}
        override fun onDown(e: MotionEvent): Boolean { return true }

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            gestureInterfacePar.setOnScroll(e1, e2, distanceX, distanceY)
            return false
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            return super.onFling(e1, e2, velocityX, velocityY)
        }
    }


    var tmpChart = 0
    var tmpChartIdx = 0

    private companion object {
        const val TIME_KEY = "TIME_KEY"
        const val CHART_KEY = "CHART_KEY"
        const val CHART_IDX_KEY = "CHART_IDX_KEY"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        metronome = Metronome(applicationContext,
            findViewById(R.id.swMetronome),findViewById(R.id.bMute),
            findViewById(R.id.tvBPM),findViewById(R.id.ivBeat),
            findViewById(R.id.tvBeatClock), ::getNextChord)

        if(savedInstanceState != null) {
            time = savedInstanceState.getInt(TIME_KEY)
            tmpChart = savedInstanceState.getInt(CHART_KEY)
            tmpChartIdx = savedInstanceState.getInt(CHART_IDX_KEY)
            metronome.restoreState(savedInstanceState)
        }

        // instantiate thread handler for clock counter
        clockHandler = Handler(Looper.getMainLooper())

        ivDiagram = findViewById(R.id.ivDiagram)

        // instantiate control listeners
        findViewById<ImageView>(R.id.ivDiagram).setOnClickListener { invertDiagram() }

        findViewById<TextView>(R.id.tvTime).setOnClickListener { clockReset() }
        findViewById<TextView>(R.id.tvBeatClock).setOnClickListener { beatReset() }

        findViewById<MaterialButton>(R.id.bMute).setOnClickListener { metronome.muteClicked() }

        findViewById<Button>(R.id.bChartLeft).setOnClickListener {
            cdDiagram.chords.prevChart()
            initChart()
        }

        findViewById<Button>(R.id.bChartRight).setOnClickListener {
            cdDiagram.chords.nextChart()
            initChart()
        }


        // ----------------------------------------------------------------------------
        // implement touchable tvChordList

        val mGestureDetector = GestureDetector(this, MyGestureDetector(object : GestureInterface {
            override fun setOnScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float) {
                //handle the scroll
                //Log.d("test","scroll $distanceX x $distanceY")
            }
            override fun onClick(e: MotionEvent) {
                //handle the single click
                val tv = findViewById<TextView>(R.id.tvChordList)
                val txt = tv.text
                val widths = FloatArray(txt.length)
                val i = tv.paint.getTextWidths(txt.toString(), widths)
                var j = 0
                var sum = 0f
                while(sum < e.x && j < i) {
                    sum += widths[j]
                    j += 1
                }
                val tokens = txt.substring(0,j).split("[ |]+".toRegex()).size
                if(tokens > 1)
                    for(k in 2..tokens) getNextChord()
            }
        }))

        findViewById<View>(R.id.tvChordList).setOnTouchListener { v, event ->
            v.performClick()
            mGestureDetector.onTouchEvent(event)
        }

        // ----------------------------------------------------------------------------

        findViewById<Button>(R.id.swMetronome).setOnClickListener {
            metronome.play()
            updateClock()
        }
        findViewById<Slider>(R.id.slBPM).addOnChangeListener { _, value, _ ->
            metronome.updateBpm(value.toInt())
        }

        // immediately after creation, instantiate the chord diagram object
        window.decorView.post {
            cdDiagram = ChordDiagram(this, ivDiagram, findViewById(R.id.tvChordTitle), tmpChart, tmpChartIdx)
            if(savedInstanceState == null) {
                cdDiagram.diagram("C", "M7", 0)
                initChart()
            } else {
                findViewById<SwitchCompat>(R.id.swMetronome).isChecked = false
                findViewById<TextView>(R.id.tvChart).text = cdDiagram.chords.chartName
                metronome.displayBeat()
                displayTime()
                getNextChord(redrawLastChord = true)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(TIME_KEY,time)
        outState.putInt(CHART_KEY,cdDiagram.chords.chart)
        outState.putInt(CHART_IDX_KEY,cdDiagram.chords.chartIdx)
        metronome.saveState(outState)
        updateClock()
    }


    // ----------------------------------------------------------------------------------------
    // Thread Handlers

    private val updateClockTask = object : Runnable {
        override fun run() {
            if(!metronome.enabled) return
            clockHandler.postDelayed(this, 1000)
            displayTime()
            time += 1
        }
    }


    // ----------------------------------------------------------------------------------------
    // Widget Access/Listeners

    private fun invertDiagram() {
        cdDiagram.changeColorMode()
    }

    private fun clockReset() {
        time = 0
        findViewById<TextView>(R.id.tvTime).text = getString(R.string.time_zero)
    }

    private fun beatReset() {
        metronome.beatReset()
        initChart()
    }

    private fun displayTime() {
        findViewById<TextView>(R.id.tvTime).text =
            String.format("%d:%02d:%02d", time / 3600, time / 60, time % 60)
    }

    // ----------------------------------------------------------------------------------------


    private fun initChart() {
        findViewById<TextView>(R.id.tvChart).text = cdDiagram.chords.chartName
        cdDiagram.chords.chartIdx = 1
        metronome.skipChordFlag = false
        metronome.beatReset()
        metronome.displayBeat()
        getNextChord()
    }

    private fun updateClock() {
        if(clockHandler.hasCallbacks(updateClockTask)) clockHandler.removeCallbacks(updateClockTask)
        if(metronome.enabled) clockHandler.postDelayed(updateClockTask, 1000)
    }

    fun getNextChord(redrawLastChord: Boolean = false) {
        val rtp: Triple<String,String,Int>

        if(cdDiagram.chords.chart == -1)
            rtp = cdDiagram.chords.getRandomChord()
        else {
            if(redrawLastChord)
                cdDiagram.chords.prevChord()
            rtp = cdDiagram.chords.chordToRTP()
            cdDiagram.chords.nextChord()
        }

        cdDiagram.diagram(rtp)

        val a = "${rtp.first}${rtp.second}"
        var b = if (cdDiagram.chords.chartIdx == 1) "" else "   " + cdDiagram.chords.chartList(-1)
        val c = cdDiagram.chords.chartList()
        while(b.length < 200) b += "  |  $c"

        // make the chord chart string with the current chord highlighted
        val str = SpannableStringBuilder(a + b)
        str.setSpan(ForegroundColorSpan(getColor(R.color.green_active)),0, a.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        findViewById<TextView>(R.id.tvChordList).text = str
    }

}