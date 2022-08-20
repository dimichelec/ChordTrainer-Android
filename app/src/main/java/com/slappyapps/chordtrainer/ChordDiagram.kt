package com.slappyapps.chordtrainer

import android.content.Context
import android.graphics.*
import android.util.TypedValue
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.drawable.toBitmap
import com.google.android.material.color.MaterialColors
import kotlin.math.abs


class ChordDiagram(private val context: Context, private val iView: ImageView,
                   private val tvTitle: TextView, chart: Int, chartIndex: Int) {

    val chords = Chords(chart, chartIndex)

    // find the scale of the diagram based on the width of the ImageView it is being drawn in
    private val scale = iView.width/330f
    private val diagramPos = arrayOf(5.6f,0.3f).map { scale*it.dp2px }

    private val stringWidths = arrayOf(0.65f,0.50f,0.36f,0.26f,0.18f,0.13f).map { scale*it.dp2px }

    // neck metrics
    private val frets: Int = 5
    private val fretsPitch = scale*18f.dp2px
    private val stringOverflow = scale*3.6f.dp2px
    private val stringLength = frets * fretsPitch + stringOverflow
    private val stringsPitch = scale*11.5f.dp2px
    private val neckWidth = stringsPitch*5f
    private val dotRadius = scale*3f.dp2px
    private val dotThickness = scale*0.2f.dp2px
    private val nutThickness = scale*3f.dp2px
    private val fretThickness = scale*0.4f.dp2px

    private val fretboardRect = arrayOf(
        diagramPos[0] + scale*24.7f.dp2px, diagramPos[1] + scale*17f.dp2px,
        diagramPos[0] + scale*24.7f.dp2px + neckWidth, diagramPos[1] + scale*17f.dp2px + stringLength)

    private val openStringTop = fretboardRect[1]

    private val firstFretFontSize = scale*7f.sp2px
    private val firstFretPos = arrayOf(
        fretboardRect[2] + scale*9f.dp2px,
        fretboardRect[1] + fretsPitch/2f + firstFretFontSize/2f)

    // note metrics
    private val noteRadius = scale*5.3f.dp2px
    private val noteFontSize = scale*6.5f.sp2px
    private val noteFontSizeSmall = scale*5f.sp2px

    private var backgroundColor = MaterialColors.getColor(iView, android.R.attr.colorBackground)
    private var textColor = MaterialColors.getColor(iView, android.R.attr.textColorPrimary)

    private lateinit var lastDiagram:Triple<String,String,Int>

    private val Float.dp2px: Float
        get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,this,context.resources.displayMetrics)

    private val Float.sp2px: Float
        get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,this,context.resources.displayMetrics)

    fun changeColorMode() {

        val bg = MaterialColors.getColor(iView, android.R.attr.colorBackground)
        val fg = MaterialColors.getColor(iView, android.R.attr.textColorPrimary)

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

        val firstFret: Int = if (first_fret_in == 0) 1 else first_fret_in

        // clear area
        paint.color = backgroundColor
        canvas.drawRect(0f,0f,iView.width.toFloat(),iView.height.toFloat(),paint)

        // draw first fret line
        var x0: Float = fretboardRect[0]
        var x1: Float = fretboardRect[2]
        var y: Float = fretboardRect[1]

        var firstFretStr = ""

        paint.color = textColor
        if(firstFret == 1) {
            // nut
            paint.strokeWidth = nutThickness
            paint.strokeCap = Paint.Cap.SQUARE
            canvas.drawLine(x0,y,x1,y,paint)
        } else {
            // first fret offset line
            paint.strokeWidth = fretThickness
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
        for(w in stringWidths) {
            paint.strokeWidth = w
            canvas.drawLine(x, y0, x, y1, paint)
            x += stringsPitch
        }

        // frets
        x0 = fretboardRect[0]
        x1 = fretboardRect[2]
        y = fretboardRect[1] + fretsPitch

        paint.strokeCap = Paint.Cap.BUTT
        paint.strokeWidth = fretThickness
        for(i in 0 until frets) {
            canvas.drawLine(x0, y, x1, y, paint)
            y += fretsPitch
        }

        // dots
        x = (fretboardRect[0] + fretboardRect[2])/2f
        y0 = fretboardRect[1] + fretsPitch/2f
        val dottedFrets = arrayOf(3,5,7,9,15,17,19,21)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dotThickness
        for(fret in dottedFrets) {
            y = y0 + fretsPitch * (fret-firstFret)
            if(y > fretboardRect[1] && y < fretboardRect[3])
                canvas.drawCircle(x,y,dotRadius,paint)
        }

        // 12th fret dots
        y = y0 + fretsPitch * (12 - firstFret)
        if(y > fretboardRect[1] && y < fretboardRect[3]) {
            canvas.drawCircle(x -stringsPitch, y, dotRadius, paint)
            canvas.drawCircle(x +stringsPitch, y, dotRadius, paint)
        }

        // print first fret offset if there is one
        paint.style = Paint.Style.FILL
        paint.textSize = firstFretFontSize
        if(firstFret != 1)
            canvas.drawText(firstFretStr, firstFretPos[0], firstFretPos[1], paint)

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

        val x = fretboardRect[0] + stringsPitch*string
        val textOffs = scale*1f.dp2px
        if(fret == -1) {    // 'X' string not played
            val y = openStringTop
            paint.textSize = noteFontSize
            paint.color = textColor
            canvas.drawText("X",x-paint.measureText("X")/2f,y-paint.textSize/2f-textOffs,paint)
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
            paint.color = Color.WHITE
            canvas.drawText(txt,x-paint.measureText(txt)/2f,y+paint.textSize/2f-textOffs,paint)
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
        tvTitle.text = context.resources.getString(R.string.chord_title_format,root,type,rootString+1)
    }
}