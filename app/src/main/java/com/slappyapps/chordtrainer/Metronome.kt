package com.slappyapps.chordtrainer

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

class Metronome(private var applicationContext: Context,
                private var swEnable: SwitchCompat, private var bMute: MaterialButton,
                private var tvBpm: TextView, private var ivBeatIndicator: ImageView,
                private var tvBeatClock: TextView, getNextChord: () -> Unit) {

    private val metronomeClickA: MediaPlayer = MediaPlayer.create(applicationContext, R.raw.bbclick)
    private val metronomeClickB: MediaPlayer = MediaPlayer.create(applicationContext, R.raw.bbclick)
    private var beatIndicatorDelay: Long = 0
    private var beatColorHigh = ContextCompat.getColor(applicationContext, R.color.green_active)
    private var beatColorLow = ContextCompat.getColor(applicationContext, R.color.green_active_dim)

    private var bpm = 90
    private var mute = false
    var enabled = false
    private var measure = 0
    private var beat = 0
    private var firstBeat = true
    var skipChordFlag = false

    private var metronomeHandler: Handler = Handler(Looper.getMainLooper())

    init {
        // set volumes
        metronomeClickA.setVolume(.9f,.9f)
        metronomeClickB.setVolume(.2f,.2f)
    }

    private companion object {
        const val MEASURE_KEY = "MEASURE_KEY"
        const val BEAT_KEY = "BEAT_KEY"
        const val MUTE_KEY = "MUTE_KEY"
    }

    fun restoreState(bundle: Bundle?) {
        enabled = false
        measure = bundle!!.getInt(MEASURE_KEY)
        beat = bundle.getInt(BEAT_KEY)
        mute = bundle.getBoolean(MUTE_KEY)
        bMute.isChecked = mute
        muteIcon()
    }

    fun saveState(bundle: Bundle?) {
        enabled = false
        bundle?.putInt(MEASURE_KEY,measure)
        bundle?.putInt(BEAT_KEY,beat)
        bundle?.putBoolean(MUTE_KEY,mute)
        swEnable.isChecked = false
    }

    private fun muteIcon() {
        bMute.icon =
            AppCompatResources.getDrawable(applicationContext,
                if(mute) R.drawable.ic_mute_24 else R.drawable.ic_speaker_24)
    }

    fun muteClicked() {
        mute = bMute.isChecked
        muteIcon()
    }

    fun displayBeat() {
        tvBeatClock.text = applicationContext.getString(R.string.beat_format,measure,beat)
    }

    private val callback = Runnable {   // updateBeatTask
        if(enabled) {
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
            click(theOne)
            displayBeat()
            next()
        }
    }


    private fun update() {
        if(metronomeHandler.hasCallbacks(callback)) metronomeHandler.removeCallbacks(callback)
        if(enabled) metronomeHandler.postDelayed(callback, (60000f/bpm).toLong())
        tvBpm.text = applicationContext.getString(R.string.bpm_format,bpm)
        beatIndicatorDelay = (30000f/bpm).toLong()
    }

    fun play() {
        enabled = swEnable.isChecked
        firstBeat = true
        update()
    }

    fun next() {
        metronomeHandler.postDelayed(callback, (60000f / bpm).toLong())
    }

    fun beatReset() {
        measure = 1
        beat = 1
        firstBeat = true
    }

    private fun updateBeat(): Boolean {
        beat = if (beat == 4) 1 else beat + 1
        measure += if (beat == 1) 1 else 0
        return (beat == 1)
    }

    fun updateBpm(value: Int) {
        bpm = value
        update()
    }

    private fun click(theOne: Boolean = false) {
        if(metronomeClickA.isPlaying) metronomeClickA.stop()
        if(metronomeClickB.isPlaying) metronomeClickB.stop()
        if(theOne) {
            if(!mute) metronomeClickA.start()
            ivBeatIndicator.setColorFilter(beatColorHigh)
        } else {
            if(!mute) metronomeClickB.start()
            ivBeatIndicator.setColorFilter(beatColorLow)
        }
        ivBeatIndicator.visibility = View.VISIBLE
        Handler(Looper.getMainLooper()).postDelayed({
            ivBeatIndicator.colorFilter = null
            ivBeatIndicator.visibility = View.INVISIBLE
        },
            beatIndicatorDelay
        )
    }



}