package com.luke.flyricviewdemo.util

import android.content.Context

fun Context.getDisplayWidth(): Int {
    return applicationContext.resources.displayMetrics.widthPixels
}

fun Context.getDisplayHeight(): Int {
    return applicationContext.resources.displayMetrics.heightPixels
}