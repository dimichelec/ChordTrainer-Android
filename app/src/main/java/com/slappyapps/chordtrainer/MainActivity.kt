package com.slappyapps.chordtrainer

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.slider.Slider



class MainActivity : AppCompatActivity() {

    private lateinit var ivDiagram: ImageView
    private lateinit var cdDiagram: ChordDiagram
    private var chart = 0
    private var chartIdx = 0
    private var metronomeOn = false
    private var measure = 0
    private var time = 0
    private var beat = 0
    private var firstBeat = true
    private var skipChordFlag = false
    private lateinit var metronomeClickA: MediaPlayer
    private lateinit var metronomeClickB: MediaPlayer
    private var bpm = 90

    private lateinit var metronomeHandler: Handler
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


    private companion object {
        const val CHART_KEY = "CHART_KEY"
        const val CHART_IDX_KEY = "CHART_IDX_KEY"
        const val BEAT_KEY = "BEAT_KEY"
        const val MEASURE_KEY = "MEASURE_KEY"
        const val TIME_KEY = "TIME_KEY"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if(savedInstanceState != null) {
            metronomeOn = false
            chart = savedInstanceState.getInt(CHART_KEY)
            chartIdx = savedInstanceState.getInt(CHART_IDX_KEY)
            beat = savedInstanceState.getInt(BEAT_KEY)
            measure = savedInstanceState.getInt(MEASURE_KEY)
            time = savedInstanceState.getInt(TIME_KEY)
        }

        // init MediaPlayers for metronome
        metronomeClickA = MediaPlayer.create(applicationContext, R.raw.bbclick)
        metronomeClickA.setVolume(.9f,.9f)

        metronomeClickB = MediaPlayer.create(applicationContext, R.raw.bbclick)
        metronomeClickB.setVolume(.2f,.2f)

        // instantiate thread handlers for metronome and clock counter
        metronomeHandler = Handler(Looper.getMainLooper())
        clockHandler = Handler(Looper.getMainLooper())

        ivDiagram = findViewById(R.id.ivDiagram)

        // instantiate control listeners
        findViewById<ImageView>(R.id.ivDiagram).setOnClickListener { invertDiagram() }

        findViewById<TextView>(R.id.tvTime).setOnClickListener { clockReset() }
        findViewById<TextView>(R.id.tvBeatClock).setOnClickListener { beatReset() }

        findViewById<Button>(R.id.bChartLeft).setOnClickListener { prevChart() }
        findViewById<Button>(R.id.bChartRight).setOnClickListener { nextChart() }


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

        findViewById<Button>(R.id.swMetronome).setOnClickListener { metronomePlay() }
        findViewById<Slider>(R.id.slBPM).addOnChangeListener { _, value, _ ->
            metronomeBPMSlide(value)
        }

        // immediately after creation, instantiate the chord diagram object
        window.decorView.post {
            cdDiagram = ChordDiagram(this, ivDiagram, findViewById(R.id.tvChordTitle))
            if(savedInstanceState == null) {
                cdDiagram.diagram("C", "M7", 0)
                initChart()
            } else {
                findViewById<SwitchCompat>(R.id.swMetronome).isChecked = false
                findViewById<TextView>(R.id.tvChart).text = cdDiagram.chords.chordProgressions[chart][0]
                displayBeat()
                displayTime()
                getNextChord(redrawLastChord = true)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(CHART_KEY,chart)
        outState.putInt(CHART_IDX_KEY,chartIdx)
        outState.putInt(BEAT_KEY,beat)
        outState.putInt(MEASURE_KEY,measure)
        outState.putInt(TIME_KEY,time)

        metronomeOn = false
        findViewById<SwitchCompat>(R.id.swMetronome).isChecked = false
        updateMetronome()
        updateClock()
    }


    // ----------------------------------------------------------------------------------------
    // Thread Handlers

    private val updateBeatTask = object : Runnable {
        override fun run() {
            if(metronomeOn) {
                var theOne = false
                if(skipChordFlag) {
                    beat = 1
                    skipChordFlag = false
                    theOne = true
                } else if(firstBeat) {
                    firstBeat = false
                    theOne = (beat == 1)
                } else {
                    if (updateBeat()) {
                        getNextChord()
                        theOne = true
                    }
                }
                clickMetronome(theOne)
                displayBeat()
                metronomeHandler.postDelayed(this, (60000f / bpm).toLong())
            }
        }
    }

    private val updateClockTask = object : Runnable {
        override fun run() {
            if(!metronomeOn) return
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
        measure = 1
        beat = 1
        firstBeat = true
        initChart()
    }

    private fun prevChart() {
        chart = if(chart == 0) cdDiagram.chords.chordProgressions.size-1 else chart -1
        initChart()
    }

    private fun nextChart() {
        chart = if(chart >= (cdDiagram.chords.chordProgressions.size-1)) 0 else chart +1
        initChart()
    }

    private fun metronomePlay() {
        metronomeOn = findViewById<SwitchCompat>(R.id.swMetronome).isChecked
        firstBeat = true
        updateMetronome()
        updateClock()
    }

    private fun metronomeBPMSlide(value: Float) {
        bpm = value.toInt()
        updateMetronome()
    }

    private fun displayBeat() {
        findViewById<TextView>(R.id.tvBeatClock).text = getString(R.string.beat_format,measure,beat)
    }

    private fun displayTime() {
        findViewById<TextView>(R.id.tvTime).text =
            String.format("%d:%02d:%02d", time / 3600, time / 60, time % 60)
    }

    // ----------------------------------------------------------------------------------------


    private fun initChart() {
        findViewById<TextView>(R.id.tvChart).text = cdDiagram.chords.chordProgressions[chart][0]
        chartIdx = 1
        skipChordFlag = false
        beat = 1
        measure = 1
        firstBeat = true
        displayBeat()
        getNextChord()
    }

    private fun updateMetronome() {
        if(metronomeHandler.hasCallbacks(updateBeatTask)) metronomeHandler.removeCallbacks(updateBeatTask)
        if(metronomeOn) metronomeHandler.postDelayed(updateBeatTask, (60000f/bpm).toLong())
        findViewById<TextView>(R.id.tvBPM).text = getString(R.string.bpm_format,bpm)
    }

    private fun clickMetronome(theOne: Boolean = false) {
        if(metronomeClickA.isPlaying) metronomeClickA.stop()
        if(metronomeClickB.isPlaying) metronomeClickB.stop()
        if(theOne)
            metronomeClickA.start()
        else
            metronomeClickB.start()
    }

    private fun updateBeat(): Boolean {
        beat = if (beat == 4) 1 else beat + 1
        measure += if (beat == 1) 1 else 0
        return (beat == 1)
    }

    private fun updateClock() {
        if(clockHandler.hasCallbacks(updateClockTask)) clockHandler.removeCallbacks(updateClockTask)
        if(metronomeOn) clockHandler.postDelayed(updateClockTask, 1000)
    }

    private fun getNextChord(redrawLastChord: Boolean = false) {
        val rtp: Triple<String,String,Int>

        if(chart == -1)
            rtp = cdDiagram.chords.getRandomChord()
        else {
            if(redrawLastChord) {
                chartIdx -= 1
                if (chartIdx == 0) chartIdx = cdDiagram.chords.chordProgressions[chart].size-1
            }
            rtp = cdDiagram.chords.chordToRTP(chart,chartIdx)
            chartIdx += 1
            if (chartIdx >= cdDiagram.chords.chordProgressions[chart].size)
                chartIdx = 1
        }

        cdDiagram.diagram(rtp)

        val a = "${rtp.first}${rtp.second}"
        var b = if (chartIdx == 1) "" else "   " + cdDiagram.chords.chartList(chart, chartIdx-1)
        val c = cdDiagram.chords.chartList(chart)
        while(b.length < 200) b += "  |  $c"

        // make the chord chart string with the current chord highlighted
        val str = SpannableStringBuilder(a + b)
        str.setSpan(ForegroundColorSpan(getColor(R.color.green_active)),0, a.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        findViewById<TextView>(R.id.tvChordList).text = str
    }

}