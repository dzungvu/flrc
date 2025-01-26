package com.luke.mediamixer2.audio_mixer.audio_container

import android.media.MediaCodec.BufferInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import androidx.annotation.RequiresApi
import com.luke.mediamixer2.audio_mixer.Muxer
import com.luke.mediamixer2.audio_mixer.TrackType
import java.io.FileDescriptor
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Allows to write sample data.  Will also queue sample data that come in _before_ setting the
 * output format.
 */
class MP3Muxer private constructor (): Muxer {

    private lateinit var mediaMuxer: MediaMuxer


    @RequiresApi(Build.VERSION_CODES.O)
    constructor(fileDescriptor: FileDescriptor): this() {
        mediaMuxer = MediaMuxer(fileDescriptor, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

    }

    constructor(path: String) : this() {
        mediaMuxer = MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM)
    }

    private var audioFormat: MediaFormat? = null

    private var audioTrackIndex: Int? = null

    // we have to wait for encoder to be initialised (with actual output media format)
    // before it is initialised, we can hold audio & video samples in these buffers
    private var queuedByteBuffer: MutableList<ByteBuffer> = mutableListOf()
    private val queuedSampleInfoList: MutableList<SampleInfo> = mutableListOf()

    private var started: Boolean = false

    override fun setOutputFormat(trackType: TrackType, format: MediaFormat) {
        when (trackType) {
            TrackType.AUDIO -> audioFormat = format
            else -> {}
        }
        if (fullyConfigured()) onSetOutputFormat()
    }

    private fun fullyConfigured(): Boolean = audioFormat != null

    private fun getTrackIndexForSampleType(trackType: TrackType): Int =
        when (trackType) {
            TrackType.AUDIO -> audioTrackIndex
            else -> throw IllegalStateException("Track type '$trackType' not supported")
        } ?: throw IllegalStateException("No index set for '$trackType'")

    /**
     * Sets the output format and starts the [MediaMuxer].  Will also apply all currently accumulated
     * sample data.
     */
    private fun onSetOutputFormat() {
        audioFormat?.let { addAudioTrack(it) }

        mediaMuxer.start()
        started = true

        // write to the muxer all data from queue
        var byteBuffer = queuedByteBuffer.firstOrNull() ?: return
        byteBuffer.flip() // switch from read to write
        var byteBufferIndex = 0

        val bufferInfo = BufferInfo()
        var offset = 0
        queuedSampleInfoList.forEach { sampleInfo ->
            val isSampleInNextBuffer = offset + sampleInfo.size > byteBuffer.limit()
            if (isSampleInNextBuffer) {
                offset = 0
                byteBufferIndex++
                byteBuffer = queuedByteBuffer.getOrNull(byteBufferIndex) ?: return@forEach
                byteBuffer.flip() // switch from read to write
            }
            sampleInfo.writeToBufferInfo(bufferInfo, offset)
            mediaMuxer.writeSampleData(
                getTrackIndexForSampleType(sampleInfo.trackType),
                byteBuffer,
                bufferInfo
            )
            offset += sampleInfo.size
        }
        queuedSampleInfoList.clear()
        queuedByteBuffer.clear()
    }

    override fun writeSampleData(
        trackType: TrackType,
        byteBuf: ByteBuffer,
        bufferInfo: BufferInfo
    ) {
        if (started) {
            mediaMuxer.writeSampleData(getTrackIndexForSampleType(trackType), byteBuf, bufferInfo)
            return
        }

        byteBuf.limit(bufferInfo.offset + bufferInfo.size)
        byteBuf.position(bufferInfo.offset)

        // save sample data in queue when muxer not ready
        // try allocate min number of byte buffers because it is memory consuming
        val byteBuffer = getNextQueuedForMuxerByteBuffer(bufferInfo.size, byteBuf.capacity())
        byteBuffer.put(byteBuf)
        queuedSampleInfoList.add(
            SampleInfo(
                trackType,
                bufferInfo.size,
                bufferInfo
            )
        )
    }

    private fun getNextQueuedForMuxerByteBuffer(sampleSize: Int, capacity: Int): ByteBuffer {

        fun createBuffer(sampleSize: Int, capacity: Int): ByteBuffer {
            val newByteBuffer = ByteBuffer.allocateDirect(Integer.max(capacity, sampleSize))
                .order(ByteOrder.nativeOrder())
            queuedByteBuffer.add(newByteBuffer)
            return newByteBuffer
        }

        if (queuedByteBuffer.isEmpty()) {
            return createBuffer(sampleSize, capacity)
        }

        val byteBuffer = queuedByteBuffer.last()

        if (byteBuffer.remaining() >= sampleSize) return byteBuffer

        return createBuffer(sampleSize, capacity)
    }

    private fun addAudioTrack(audioFormat: MediaFormat) {
        audioTrackIndex = mediaMuxer.addTrack(audioFormat)
    }


    fun release() {
        if (started) mediaMuxer.stop()
        clearState()
        mediaMuxer.release()
    }

    private fun clearState() {
        audioFormat = null
        audioTrackIndex = null
        started = false
        queuedByteBuffer.clear()
        queuedSampleInfoList.clear()
    }

    private class SampleInfo(
        val trackType: TrackType,
        val size: Int,
        bufferInfo: BufferInfo
    ) {
        private val presentationTimeUs: Long = bufferInfo.presentationTimeUs
        private val flags: Int = bufferInfo.flags

        fun writeToBufferInfo(bufferInfo: BufferInfo, offset: Int) {
            bufferInfo.set(offset, size, presentationTimeUs, flags)
        }
    }
}