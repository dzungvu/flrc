package com.luke.flyricui.utils

object FLyricUIUtil {
    fun normalize(min: Float, max: Float, value: Float, limit: Boolean = false): Float {
        if (min == max) return 1f
        return ((value - min) / (max - min)).let {
            if (limit) it.coerceIn(0f, 1f) else it
        }
    }
}