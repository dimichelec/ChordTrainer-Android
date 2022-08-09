package com.slappyapps.chordtrainer

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.util.Log
import android.util.TypedValue
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.drawable.toBitmap
import com.google.android.material.color.MaterialColors
import kotlin.math.abs


class ChordDiagram(private val context: Context, private val iView: ImageView, private val tvTitle: TextView) {

    val chords = Chords()

    private val frets: Int = 5
    private val stringWidths = arrayOf(24f, 18f, 14f, 12f, 10f, 8f)

    private val scale = iView.width/330f
    private val fretsPitch = scale*50f

    private val stringOverflow = 10f*scale
    private val stringHeight = frets * fretsPitch + stringOverflow
    private val stringsPitch = scale*30f
    private val stringsWidth = stringsPitch*5f

    private val dotRadius = scale*7f

    private val noteRadius = scale*14f
    private val noteFontSize = scale*6.5f.sp2px
    private val noteFontSizeSmall = scale*5f.sp2px

    private val diagramPos = arrayOf(4f.dp + scale*15f, 8f.dp)

    private val firstFretFontSize = scale*7f.sp2px
    private val fretboardRect = arrayOf(
        diagramPos[0] + scale*65f, diagramPos[1] + scale*45f,
        diagramPos[0] + scale*65f + stringsWidth, diagramPos[1] + scale*45f + stringHeight)
    private val firstFretPos = arrayOf(
        fretboardRect[2] + 24f*scale,
        fretboardRect[1] + fretsPitch/2f + firstFretFontSize/2f)
    private val openStringTop = fretboardRect[1]

    private var backgroundColor: Int
    private var textColor: Int

    private lateinit var lastDiagram:Triple<String,String,Int>


    private val Float.dp: Float
        get() = (this / Resources.getSystem().displayMetrics.density)

    private val Float.dp2px: Float
        get() = (this * Resources.getSystem().displayMetrics.densityDpi/160f)

    private val Float.sp2px: Float
        get() = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,this,context.resources.displayMetrics)

    init {
        // get background and foreground colors
        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
        backgroundColor = typedValue.data

        context.theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
        textColor = typedValue.data

        MaterialColors.getColor(iView, com.google.android.material.R.attr.colorOnPrimary)

        Log.d("test","${1f.dp} ${1f.dp2px} $scale ${scale*18f} ${5f.sp2px}")
    }


    fun changeColorMode() {

        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
        val bg = typedValue.data

        context.theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
        val fg = typedValue.data

        if(backgroundColor == bg) {
            backgroundColor = fg
            textColor = bg
            tvTitle.setTextColor(bg)
            tvTitle.setBackgroundColor(fg)
        } else {
            backgroundColor = bg
            textColor = fg
            tvTitle.setTextColor(fg)
            tvTitle.setBackgroundColor(bg)
        }

        diagram(lastDiagram)
    }

    private fun drawBlank(first_fret_in:Int=1) {

        val bitmap = Bitmap.createBitmap(iView.width, iView.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint()
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 1f

        val firstFret: Int = if (first_fret_in == 0) 1 else first_fret_in

        // clear area
        paint.color = backgroundColor
        canvas.drawRect(0f,0f, iView.width.toFloat(), iView.height.toFloat(),paint)

        // draw first fret line
        var x0: Float = fretboardRect[0]
        var x1: Float = fretboardRect[2]
        var y: Float = fretboardRect[1]

        var firstFretStr = ""

        paint.color = textColor
        if(firstFret == 1) {
            // nut
            paint.strokeWidth = 60f.dp
            paint.strokeCap = Paint.Cap.SQUARE
            canvas.drawLine(x0,y,x1,y,paint)
        } else {
            // first fret offset line
            paint.strokeWidth = 4f.dp
            paint.strokeCap = Paint.Cap.BUTT
            canvas.drawLine(x0,y,x1,y,paint)

            // first fret text for printing later
            firstFretStr = "$firstFret" + when(firstFret) {
                2 -> "nd"
                3 -> "rd"
                else -> "th"
            }
        }

        // strings
        var x: Float = fretboardRect[0]
        var y0: Float = fretboardRect[3]
        var y1: Float = fretboardRect[1]
        if(firstFret != 1) y1 -= stringOverflow

        paint.strokeCap = Paint.Cap.BUTT
        for(i in 0..5) {
            paint.strokeWidth = stringWidths[i].dp
            canvas.drawLine(x, y0, x, y1, paint)
            x += stringsPitch
        }

        // frets
        x0 = fretboardRect[0]
        x1 = fretboardRect[2]
        y = fretboardRect[1] + fretsPitch

        paint.strokeCap = Paint.Cap.BUTT
        paint.strokeWidth = 4f.dp
        for(i in 0 until frets) {
            canvas.drawLine(x0, y, x1, y, paint)
            y += fretsPitch
        }

        // dots
        x = (fretboardRect[0] + fretboardRect[2]) / 2
        y0 = fretboardRect[1] + fretsPitch/2
        val dottedFrets = arrayOf(3,5,7,9,15,17,19,21)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 6f.dp
        for(fret in dottedFrets) {
            y = y0 + fretsPitch * (fret - firstFret)
            if(y > fretboardRect[1] && y < fretboardRect[3])
                canvas.drawCircle(x,y,dotRadius,paint)
        }

        // 12th fret dots
        y = y0 + fretsPitch * (12 - firstFret)
        if(y > fretboardRect[1] && y < fretboardRect[3]) {
            canvas.drawCircle(x - stringsPitch,y,dotRadius,paint)
            canvas.drawCircle(x + stringsPitch,y,dotRadius,paint)
        }

        // print first fret offset if there is one
        paint.style = Paint.Style.FILL
        paint.textSize = firstFretFontSize
        if(firstFret != 1)
            canvas.drawText(firstFretStr,firstFretPos[0],firstFretPos[1],paint)

        iView.setImageBitmap(bitmap)
    }

    private fun drawNote(canvas:Canvas,string:Int,fret:Int,interval:Int=0) {

        fun intervalAscii(i:Int):String {
            return (
                (if(i in arrayOf(4,5,11)) "p" else if(i < 0) "b" else "")
              + (if(i == 1) "R" else "${abs(i)}"))
        }

        val paint = Paint()
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 1f

        val x = fretboardRect[0] + stringsPitch*string
        if(fret == -1) {    // 'X' string not played
            val y = openStringTop
            paint.textSize = noteFontSize
            paint.color = textColor
            canvas.drawText("X",x-paint.measureText("X")/2f,y-paint.textSize/1.5f,paint)
        } else {
            val y = fretboardRect[1] + fretsPitch/2f + fretsPitch*(fret-1)
            paint.color = when(interval) {
                1 ->    Color.rgb(1f, .3f, .3f)
                3, 5 -> Color.rgb(.3f, .3f, 1f)
                else -> Color.rgb(.3f, .3f, .3f)
            }
            canvas.drawCircle(x,y,noteRadius,paint)
            val txt: String = intervalAscii(interval)
            paint.textSize = if(txt.length < 3) noteFontSize else noteFontSizeSmall
            paint.color = Color.WHITE   //backgroundColor
            canvas.drawText(txt,x-paint.measureText(txt)/2f,y+paint.textSize.dp/1.1f,paint)
        }
    }

    fun diagram(rtp:Triple<String,String,Int>) { this.diagram(rtp.first, rtp.second, rtp.third) }

    fun diagram(root:String,type:String,chord_form:Int=0) {

        lastDiagram = Triple(root,type,chord_form)

        val chord = chords.findBestChord(root,type,chord_form)
        val rootString = chord[0]
        val rootFret = chord[1]
        val chordTypeIdx = chord[2]
        val chordFormIdx = chord[3]

        // find root fret offset
        val rootFretOffset = rootFret - chords.frets(chordTypeIdx,chordFormIdx)[rootString]

        // find fret position of diagram
        var firstFret = 0
        if((chords.maxFret(chordTypeIdx,chordFormIdx) + rootFretOffset) > frets)
            firstFret = rootFretOffset

        // draw the blank diagram
        drawBlank(firstFret)

        val drawable = iView.drawable
        val bitmap = drawable.toBitmap()
        val canvas = Canvas(bitmap)

        // draw notes of chord
        chords.frets(chordTypeIdx,chordFormIdx).forEachIndexed { string, fret ->
            val interval = chords.intervals(chordTypeIdx,chordFormIdx)[string]
            if(fret < 0)
                drawNote(canvas,string,-1)
            else
                if(firstFret == 0)
                    drawNote(canvas,string,fret + rootFretOffset,interval)
                else
                    drawNote(canvas,string,fret + 1,interval)
        }
        iView.setImageBitmap(bitmap)
        tvTitle.text = "$root$type -${rootString+1}"
    }
}