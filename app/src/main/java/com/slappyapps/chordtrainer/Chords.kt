package com.slappyapps.chordtrainer

import kotlin.random.Random

class Chords(var chart: Int = 0, var chartIdx: Int = 0) {

   // the standard tuning 3 bass strings of a guitar
    private val bassTuning = arrayOf("E","A","D")
    private val notes = arrayOf("C","C#","D","D#","E","F","F#","G","G#","A","A#","B")

    /* formula_names: Chord type <M = major, m = minor>
    // formulas:     The following pattens are in order from root note on low E string, A string...
    //   (fret pattern by string, low E first, -1 = don't play, 0 = play open),
    //   (interval of fretted strings from root, 0 = string not played, 1 = root)
    //
    // array name gives chord type: M = major, m = minor, ..., followed by
    // single-digit root string, 0 = low E, 1 = A, 2 = D, ...
    // numeric array gives 6 chord fret positions starting w/ low E. -1 is an unplayed string,
    // 0 is an open string, ..., followed by 6 intervals. 1 = root, 2 = 2nd, ..., a negative
    // number indicates a flat interval. i.e., -3 is a flat 3rd
    //
    // Here's a good online chord DB: https://chords.gock.net/
     */
    private val formulaNames = arrayOf("M","m","M7","7","m7","m7b5","M9","9","m9","M11","11","m11")
    private val formulas = arrayOf(
        arrayOf(    // "M"
            intArrayOf( 0, 2, 2, 1,-1,-1,  1, 5, 1, 3, 0, 0),  // root on low E string
            intArrayOf(-1, 0, 2, 2, 2,-1,  0, 1, 5, 1, 3, 0),  // root on A string
            intArrayOf(-1,-1, 0, 2, 3, 2,  0, 0, 1, 5, 1, 3)   // root on D string
        ),
        arrayOf(    // "m"
            intArrayOf( 0, 2, 2, 0,-1,-1,  1, 5, 1,-3, 0, 0),  // root on low E string
            intArrayOf(-1, 0, 2, 2, 1,-1,  0, 1, 5, 1,-3, 0),  // root on A string
            intArrayOf(-1,-1, 0, 2, 3, 1,  0, 0, 1, 5, 1,-3)   // root on D string
        ),
        arrayOf(    // "M7"
            intArrayOf( 0,-1, 1, 1, 0,-1,  1, 0, 7, 3, 5, 0),  // root on low E string
            intArrayOf(-1, 0, 2, 1, 2, 0,  0, 1, 5, 7, 3, 5),  // root on A string
            intArrayOf(-1,-1, 0, 2, 2, 2,  0, 0, 1, 5, 7, 3)   // root on D string
        ),
        arrayOf(    // "7"
            intArrayOf( 0,-1, 0, 1, 0,-1,  1, 0,-7, 3, 5, 0),  // root on low E string
            intArrayOf(-1, 2, 1, 2, 0,-1,  0, 1, 3,-7, 1, 0),  // root on A string
            intArrayOf(-1,-1, 0, 2, 1, 2,  0, 0, 1, 5,-7, 3)   // root on D string
        ),
        arrayOf(    // "m7"
            intArrayOf( 0,-1, 0, 0, 0,-1,  1, 0,-7,-3, 5, 0),  // root on low E string
            intArrayOf(-1, 0, 2, 0, 1,-1,  0, 1, 5,-7,-3, 0),  // root on A string
            intArrayOf(-1,-1, 0, 2, 1, 1,  0, 0, 1, 5,-7,-3)   // root on D string
        ),
        arrayOf(    // "m7b5"
            intArrayOf( 1,-1, 1, 1, 0,-1,  1, 0,-7,-3,-5, 0),  // root on low E string
            intArrayOf(-1, 0, 1, 0, 1,-1,  0, 1,-5,-7,-3, 0),  // root on A string
            intArrayOf(-1,-1, 0, 1, 1, 1,  0, 0, 1,-5,-7,-3)   // root on D string
        ),
        arrayOf(    // "M9"
            intArrayOf( 1, 0, 2, 0,-1,-1,  1, 3, 7, 9, 0, 0),  // root on low E string
            intArrayOf(-1, 1, 0, 2, 1,-1,  0, 1, 3, 7, 9, 0),  // root on A string
            intArrayOf(-1,-1, 1, 0, 3, 1,  0, 0, 1, 3, 7, 9)   // root on D string
        ),
        arrayOf(    // "9"
            intArrayOf( 3,-1, 3, 2, 0,-1,  1, 0,-7, 9, 3, 0),  // root on low E string
            intArrayOf(-1, 1, 0, 1, 1, 1,  0, 1, 3,-7, 9, 5),  // root on A string
            intArrayOf(-1,-1, 1, 0, 2, 1,  0, 0, 1, 3,-7, 9)   // root on D string
        ),
        arrayOf(    // "m9"
            intArrayOf( 2, 0, 2, 1,-1,-1,  1,-3,-7, 9, 0, 0),  // root on low E string
            intArrayOf(-1, 2, 0, 2, 2,-1,  0, 1,-3,-7, 9, 0),  // root on A string
            intArrayOf(-1,-1, 2, 0, 3, 2,  0, 0, 1,-3,-7, 9)   // root on D string
        ),
        arrayOf(    // "M11"
            intArrayOf( 2,-1, 3, 1, 0,-1,  1, 0, 7, 9,11, 0),  // root on low E string
            intArrayOf(-1, 2,-1, 3, 2, 0,  0, 1, 0, 7, 9,11),  // root on A string
            intArrayOf(-1,-1, 4, 3, 0, 1,  0, 0, 1, 3,11, 7)   // root on D string
        ),
        arrayOf(    // "11"
            intArrayOf( 2,-1, 2, 3, 0,-1,  1, 0,-7, 3,11, 0),  // root on low E string
            intArrayOf(-1, 2, 1, 2, 0, 0,  0, 1, 3,-7, 1,11),  // root on A string
            intArrayOf(-1,-1, 4, 3, 0, 0,  0, 0, 1, 3,11,-7)   // root on D string
        ),
        arrayOf(    // "m11"
            intArrayOf( 2,-1, 2, 2, 0,-1,  1, 0,-7,-3,11, 0),  // root on low E string
            intArrayOf(-1, 2,-1, 2, 3, 0,  0, 1, 0,-7,-3,11),  // root on A string
            intArrayOf(-1,-1, 0, 0, 1, 1,  0, 0, 1,11,-7,-3)   // root on D string
        )
    )

    private val chordProgressions = arrayOf(
        arrayOf("Major 7s Exercise",       "GM7-1","CM7-2","FM7-3","GM7-3","DM7-2","AM7-1"),
        arrayOf("Minor 7s Exercise",       "Gm7-1","Cm7-2","Fm7-3","Gm7-3","Dm7-2","Am7-1"),
        arrayOf("Dominant 7s Exercise",    "G7-1","C7-2","F7-3","G7-3","D7-2","A7-1"),
        arrayOf("Major 9s Exercise",       "GM9-1","CM9-2","FM9-3","GM9-3","DM9-2","AM9-1"),
        arrayOf("Minor 9s Exercise",       "Gm9-1","Cm9-2","Fm9-3","Gm9-3","Dm9-2","Am9-1"),
        arrayOf("Dominant 9s Exercise",    "G9-1","C9-2","F9-3","G9-3","D9-2","A9-1"),
        arrayOf("Gmaj ii-V-I",             "Am7-1","D7-2","GM7-1"),
        arrayOf("C F Bb ii-V-Is",          "Dm7","G7","CM7","Gm7","C7","FM7","Cm7","F7","BbM7"),
        arrayOf("Eb Ab Db ii-V-Is",        "Fm7","Bb7","EbM7","Bbm7","Eb7","AbM7","Ebm7","Ab7","DbM7"),
        arrayOf("Gb B E ii-V-Is",          "Abm7","Db7","GbM7","C#m7","F#7","BM7","F#m7","B7","EM7"),
        arrayOf("A D G ii-V-Is",           "Bm7","E7","AM7","Em7","A7","DM7","Am7","D7","GM7"),
        arrayOf("Wild Horses",             "Bm","G","Am","C","D","F"),
        arrayOf("Test",                    "B11","Gm11","AM11","Em7")
    )

    val chartName: String
        get() = chordProgressions[chart][0]


    fun prevChart() {
        chart = if(chart == 0) chordProgressions.size-1 else chart-1
    }

    fun nextChart() {
        chart = if(chart >= (chordProgressions.size-1)) 0 else chart+1
    }

    fun prevChord() {
        chartIdx = if(chartIdx == 1) chordProgressions[chart].size-1 else chartIdx-1
    }

    fun nextChord() {
        chartIdx = if(chartIdx >= (chordProgressions[chart].size-1)) 1 else chartIdx+1
    }

    // given the root note and chord type, return a chord from our formulas played
    // in lowest neck position
    fun findBestChord(root:String, type:String, chord_form:Int=0): Array<Int> {

        // find the forms for the chord type
        val idx = if(type == "") formulaNames.indexOf("M") else formulaNames.indexOf(type)

        // find the note index of the root note
        var iRoot = notes.indexOf(root)
        if(iRoot < 0) {
            if(root.indexOf("-") >= 0 || root.indexOf("b") >= 0)
                iRoot = notes.indexOf(root[0].toString()) - 1
            if(iRoot < 0)
                iRoot += notes.size
        }

        var out = arrayOf<Int>()
        if(chord_form == 0) {
            // find the form that will fit in the lowest position on the neck, based on
            // which string our root note will be on.
            // returns tuple (root string, root fret, index of chord in formulas, index of form in formulas chord array)
            var rootString = 0
            var iForm = 0
            for(open_note in bassTuning) {
                val iOpen = notes.indexOf(open_note)
                var rootFret = if(iRoot >= iOpen) iRoot - iOpen else 12 + iRoot - iOpen
                if(formulas[idx][iForm][6+rootString] == 1) {
                    if(rootFret < formulas[idx][iForm][rootString])
                        rootFret += 12
                    if(out.isEmpty() || rootFret < out[1])
                        out = arrayOf(rootString, rootFret, idx, iForm)
                    iForm++
                }
                rootString += 1
            }
        } else {
            // chord_form is not zero, use prescribed chord_form
            val iOpen = notes.indexOf(bassTuning[chord_form-1])
            var rootFret = if(iRoot >= iOpen) iRoot - iOpen else 12 + iRoot - iOpen
            if(rootFret < formulas[idx][chord_form-1][chord_form-1])
                rootFret += 12
            out = arrayOf(chord_form-1, rootFret, idx, chord_form-1)
        }
        return out
    }

    fun frets(type_idx: Int, chord_form: Int): IntArray {
        return formulas[type_idx][chord_form].slice(0..5).toIntArray()
    }

    fun intervals(type_idx: Int, chord_form: Int): IntArray {
        return formulas[type_idx][chord_form].slice(6..11).toIntArray()
    }

    fun maxFret(type_idx: Int, chord_form: Int): Int {
        return formulas[type_idx][chord_form].slice(0..5).max()
    }

    fun getRandomChord(): Triple<String,String,Int> {
        return Triple(
            notes[Random.nextInt(0, notes.size-1)],
            formulaNames[Random.nextInt(0, formulaNames.size-1)],
            Random.nextInt(1,3))
    }

    fun chartList(offset: Int = 0): String {
        var seq = ""
        val chords = this.chordProgressions[chart]
        val index = if(offset < 0) chartIdx + offset else offset

        chords.sliceArray(index+1 until chords.size).forEach { chord ->
            val i = chord.indexOf("-")
            seq += "${if(i != -1) chord.substring(0,i) else chord}   "
        }
        return seq.substring(0,seq.length-3)
    }

    fun chordToRTP(iChart: Int=chart, index: Int=chartIdx): Triple<String,String,Int> {
        var chord = this.chordProgressions[iChart][index]
        var position = 0
        val i = chord.indexOf("-")
        if(i != -1) {
            position = chord.substring(i+1,chord.length).toInt()
            chord = chord.substring(0,i)
        }

        var root = chord[0].toString()
        var type = ""
        if(chord.length != 1) {
            if(chord[1] == '#' || chord[1] == 'b') {
                root += chord[1]
                type = chord.substring(2,chord.length)
            } else
                type = chord.substring(1,chord.length)
        }
        return Triple(root, type, position)
    }
}
