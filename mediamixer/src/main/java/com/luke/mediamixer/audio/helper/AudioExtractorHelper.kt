package com.luke.mediamixer.audio.helper

import android.content.Context
import android.media.MediaCodec.BufferInfo
import android.media.MediaExtractor
import android.net.Uri
import com.luke.mediamixer.common.ExtractorHelper
import com.luke.mediamixer.common.TrackType

class AudioExtractorHelper(
    private val context: Context,
    private val audioFileUri: Uri,

): ExtractorHelper(context) {

    private val mExtractor: MediaExtractor? by lazy {
        try {
            createMediaExtractor(MediaExtractor(), audioFileUri)
        } catch (e: IllegalStateException) {
            null
        }
    }

    private val mAudioTrackIndex: Int by lazy {
        mExtractor?.let {
            getTrackIndex(it, TrackType.AUDIO)
        } ?: NO_INDEX
    }


    fun getAudioTrackIndex(): Int {
        return mAudioTrackIndex
    }

    fun getExtractor(): MediaExtractor? {
        return mExtractor
    }

}