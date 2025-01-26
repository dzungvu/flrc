package com.luke.mediamixer.common.model

import android.media.MediaExtractor
import android.media.MediaFormat

data class MuxerItemData (
    val mediaFormat: MediaFormat,
    val trackIndex: Int,
    val extractor: MediaExtractor
)