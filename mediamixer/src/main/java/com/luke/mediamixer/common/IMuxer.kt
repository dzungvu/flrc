package com.luke.mediamixer.common

import android.media.MediaCodec
import android.media.MediaFormat
import com.luke.mediamixer.common.model.MuxerItemData
import java.nio.ByteBuffer

interface IMuxer {
    val muxerItems: MutableList<MuxerItemData>?
    val outputPath: String
    fun writeSampleData(trackType: TrackType, byteBuf: ByteBuffer, bufferInfo: MediaCodec.BufferInfo)
    fun setOutputFormat(trackType: TrackType, format: MediaFormat)
}