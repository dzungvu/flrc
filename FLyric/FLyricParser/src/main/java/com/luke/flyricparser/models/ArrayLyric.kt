package com.luke.flyricparser.models

import com.luke.flyricparser.parser.LrcParser
import com.luke.flyricparser.parser.SimpleLrcParser
import java.io.IOException
import java.io.InputStream
import java.util.AbstractMap

open class ArrayLyric() : Lyric {

    constructor(inputStream: InputStream) : this() {
        setInputStream(inputStream)
    }


    protected val mLyric: ArrayList<AbstractMap.SimpleEntry<Long, String>> = arrayListOf()

    protected val mLyricInputStream: InputStream? = null
    open protected val mLrcParser: LrcParser<Lyric> = SimpleLrcParser()

    init {
        mLyric.add(AbstractMap.SimpleEntry(-1L, "head"))
    }

    override fun getLineIndex(milliTime: Long): Int {
        return searchOneLine(milliTime)
    }

    override fun getLine(milliTime: Long): String? {
        return getLine(searchOneLine(milliTime))
    }

    override fun getLine(index: Int): String? {
        return if (index < mLyric.size && index > 0) mLyric[index].value else null
    }

    override fun getMilliTime(index: Int): Long {
        return if (index < mLyric.size) {
            mLyric[index].key
        } else -1L
    }

    override fun append(milliTime: Long, oneLine: String?) {
        if(mLyric.isNotEmpty()) {
            if (mLyric[mLyric.size - 1].key < milliTime) {
                mLyric.add(AbstractMap.SimpleEntry(milliTime, oneLine))
            }
        } else {
            mLyric.add(AbstractMap.SimpleEntry(milliTime, oneLine))
        }
    }

    override fun remove(): AbstractMap.SimpleEntry<Long, String>? {
        if (mLyric.isNotEmpty()) {
            return mLyric.removeAt(mLyric.size - 1)
        }
        return null
    }

    override fun size(): Int {
        return mLyric.size
    }

    /**
     * Only one input stream is supported.
     * @param inputStream
     * @throws IOException
     */
    @Throws(IOException::class)
    fun setInputStream(inputStream: InputStream?) {
        if (inputStream != null) {
            mLrcParser.parseSource(this, inputStream)
        }
    }

    /**
     * Find one lyric line shown at the given time.
     * Searching by binary search algorithm.
     * @param time at what time to show the lyric line
     * @return the index of line
     */
    protected fun searchOneLine(time: Long): Int {
        var left = 0
        var right = mLyric.size
        var middle = -1
        while (left < right - 1) {
            middle = (left + right) / 2
            if (mLyric[middle].key > time) {
                right = middle
            } else {
                left = middle
            }
        }
        return if (middle == -1) -1 else left
    }
}