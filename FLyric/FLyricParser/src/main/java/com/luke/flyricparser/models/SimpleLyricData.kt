package com.luke.flyricparser.models

data class SimpleLyricData(override val metaData: LyricMetaData?, override val lyric: Lyric?) : ILyricData {
}