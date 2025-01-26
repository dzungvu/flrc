package com.luke.mediamixer.common

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log

open class ExtractorHelper(private val context: Context) {
    companion object {
        const val TAG = "ExtractorHelper"
        const val NO_INDEX = -1
    }

    fun getTrackIndex(mediaExtractor: MediaExtractor, trackType: TrackType): Int {
        val trackCount = mediaExtractor.trackCount
        for (i in 0 until trackCount) {
            val format = mediaExtractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith(trackType.prefix) == true) {
                return i
            }
        }
        Log.e(TAG, "File does not have ${trackType.prefix} track")
        return NO_INDEX
    }

    @Throws(IllegalStateException::class)
    fun createMediaExtractor(mediaExtractor: MediaExtractor, audio: Uri): MediaExtractor {
        checkNotNull(context.contentResolver.openFileDescriptor(audio, "r")) {
            "unable to acquire file descriptor for $audio"
        }.use {
            mediaExtractor.setDataSource(it.fileDescriptor)
        }
        return mediaExtractor
    }
}