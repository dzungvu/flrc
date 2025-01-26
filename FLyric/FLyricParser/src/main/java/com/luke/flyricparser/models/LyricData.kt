package com.luke.flyricparser.models

data class LyricData(
    val lyrics: ArrayList<Lyric> = arrayListOf()
) {
    data class Lyric(
        val lineNumber: Int,
        val rawValue: String,
        val type: LyricType,
        val startMs: Long,
        val endMs: Long? = null,
        val content: String,
        val words: ArrayList<Word> = arrayListOf(),
        var width: Float = 0f
    )

    data class Word(
        val index: Int,
        val startMs: Long, // time this word is started in song
        var endMs: Long?, // update this when next word is found
        val rawValue: String,
        val content: String,
        //region UI related
        var startInSentenceMs: Long, // time this word is started in sentence
        var msPerPx: Float, // ms per pixel
        var wordOffset: Float, // offset of word in sentence

        var wordInLine: Int, // a lyric can be place in multiple lines, this is the index of the line

    )

    enum class LyricType {
        SIMPLE_LRC,
        ENHANCED_LRC,
        META_DATA,
        INVALID
    }
}