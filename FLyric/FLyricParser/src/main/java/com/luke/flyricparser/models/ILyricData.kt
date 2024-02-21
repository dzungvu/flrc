package com.luke.flyricparser.models

import com.luke.flyricparser.FLRCParser

interface ILyricData {
    val metaData: LyricMetaData?
    val lyric: Lyric?
}

data class LyricMetaData (
    val dataType: FLRCParser.FileDataType? = null,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val by: String? = null,
    val language: String? = null,
    val created: String? = null,
    val modified: String? = null,
    val duration: Long? = null
)