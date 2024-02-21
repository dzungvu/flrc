package com.luke.flyricparser.models

import java.io.InputStream
import java.util.AbstractMap


class EnhancedLyric(): ArrayLyric() {

    constructor(inputStream: InputStream) : this() {
        setInputStream(inputStream)
    }

    protected var mLyricProgress: ArrayList<ArrayList<AbstractMap.SimpleEntry<Long, Int>>> = arrayListOf()

    init {
        mLyricProgress.add(ArrayList())
    }


    fun append(progress: ArrayList<AbstractMap.SimpleEntry<Long, Int>>) {
        mLyricProgress.add(progress)
    }
}