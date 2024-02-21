package com.luke.flyricparser.models

import java.util.AbstractMap

interface Lyric {
    fun getLineIndex(milliTime: Long): Int

    fun getLine(milliTime: Long): String?

    fun getLine(index: Int): String?

    fun getMilliTime(index: Int): Long

    fun append(milliTime: Long, oneLine: String?)

    fun remove(): AbstractMap.SimpleEntry<Long, String>?

    fun size(): Int
}