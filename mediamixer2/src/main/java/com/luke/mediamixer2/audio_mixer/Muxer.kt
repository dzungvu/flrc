package com.luke.mediamixer2.audio_mixer

import android.media.MediaCodec
import android.media.MediaFormat
import java.nio.ByteBuffer

interface Muxer {
    fun writeSampleData(
        trackType: TrackType,
        byteBuf: ByteBuffer,
        bufferInfo: MediaCodec.BufferInfo
    )
    fun setOutputFormat(trackType: TrackType, format: MediaFormat)
}