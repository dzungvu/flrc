package com.luke.flyricparser.parser

import com.luke.flyricparser.models.Lyric
import java.io.IOException
import java.io.InputStream


interface LrcParser<T : Lyric?> {

    @Throws(IOException::class)
    fun parseSource(lyric: T, inputStream: InputStream)
}
