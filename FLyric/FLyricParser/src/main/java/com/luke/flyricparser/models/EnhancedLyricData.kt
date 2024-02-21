package com.luke.flyricparser.models

data class EnhancedLyricData(override val metaData: LyricMetaData?, override val lyric: Lyric?): ILyricData {

}