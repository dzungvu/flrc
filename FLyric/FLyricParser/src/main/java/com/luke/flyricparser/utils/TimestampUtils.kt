package com.luke.flyricparser.utils

object TimestampUtils {
    const val MILLIS_IN_A_MINUTE = (60 * 1000).toLong()
    const val MILLIS_IN_A_SECOND: Long = 1000
    fun string2Timestamp(timeString: String): Long {
        var time: Long = 0
        val times = timeString.split(":|\\.".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        if (times.size == 3) {
            time += times[0].toInt() * MILLIS_IN_A_MINUTE
            time += times[1].toInt() * MILLIS_IN_A_SECOND
            while (times[2].length < 3) {
                times[2] += "0"
            }
            time += times[2].toInt()
        }
        return time
    }
}