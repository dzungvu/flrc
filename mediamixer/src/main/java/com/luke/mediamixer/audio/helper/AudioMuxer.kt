package com.luke.mediamixer.audio.helper

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import com.luke.mediamixer.common.IMuxer
import com.luke.mediamixer.common.TrackType
import com.luke.mediamixer.common.model.MuxerItemData
import java.nio.ByteBuffer

class AudioMuxer private constructor(builder: Builder) : IMuxer {

    private companion object {
        private const val BUFFER_CAPACITY = 1024 * 1024 //1MB
        private const val OUTPUT_BIT_RATE = 128_000
    }

    override val muxerItems = builder.muxerItems.toMutableList()
    override val outputPath = builder.outputPath

    private val bufferInfo = MediaCodec.BufferInfo()
    private val buffer = ByteBuffer.allocate(BUFFER_CAPACITY)

    private val muxer: MediaMuxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

    private var isMuxerStart = false

    fun mix() {
        write(muxerItems.removeFirst()) {
            if (muxerItems.isNotEmpty()) {
                mix()
            } else {
                stopMuxer()
            }
        }
    }

    private fun write(muxerItemData: MuxerItemData, onDone: () -> Unit) {
        val format = muxerItemData.mediaFormat
        val extractor = muxerItemData.extractor
        var trackIndex = -1
        val outputFormat = createAudioOutputFormat(format)

        if (format.getString(MediaFormat.KEY_MIME) != MediaFormat.MIMETYPE_AUDIO_AAC) {
            val decoder = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
            val encoder = MediaCodec.createEncoderByType(outputFormat.getString(MediaFormat.KEY_MIME)!!)

            decoder.configure(format, null, null, 0)
            encoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

            decoder.start()
            encoder.start()

            while (true) {
                val inputBufferIndex = decoder.dequeueInputBuffer(5_000)
                if (inputBufferIndex > 0) {
                    decoder.getInputBuffer(inputBufferIndex)?.let { inputBuffer ->
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            // End of stream, signal the decoder
                            decoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        } else {
                            decoder.queueInputBuffer(inputBufferIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    } ?: break
                }

                val outputBufferInfo = MediaCodec.BufferInfo()
                val outputBufferIndex = decoder.dequeueOutputBuffer(outputBufferInfo, 5_000)
                if (outputBufferIndex >= 0) {
                    val outputBuffer = decoder.getOutputBuffer(outputBufferIndex)

                    val encoderInputBufferIndex = encoder.dequeueInputBuffer(5_000)
                    if (encoderInputBufferIndex >= 0) {
                        encoder.getInputBuffer(encoderInputBufferIndex)?.let { encoderInputBuffer ->
                            encoderInputBuffer.clear()
                            encoderInputBuffer.put(outputBuffer)
                            encoder.queueInputBuffer(
                                encoderInputBufferIndex,
                                0,
                                outputBufferInfo.size,
                                outputBufferInfo.presentationTimeUs,
                                outputBufferInfo.flags
                            )
                        }
                        decoder.releaseOutputBuffer(outputBufferIndex, false)
                    }

                    // Get the encoded output from the encoder
                    val encoderOutputBufferInfo = MediaCodec.BufferInfo()
                    val encoderOutputBufferIndex =
                        encoder.dequeueOutputBuffer(encoderOutputBufferInfo, 5_000)
                    if (encoderOutputBufferIndex >= 0) {
                        encoder.getOutputBuffer(encoderOutputBufferIndex)
                            ?.let { encoderOutputBuffer ->
                                muxer.writeSampleData(
                                    muxerItemData.trackIndex,
                                    encoderOutputBuffer,
                                    encoderOutputBufferInfo
                                )
                            }
                        encoder.releaseOutputBuffer(encoderOutputBufferIndex, false)
                    }

                    // Break the loop when the end of stream is reached
                    if ((outputBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break
                    }
                }
            }
            trackIndex = muxer.addTrack(encoder.outputFormat)
        } else {
            trackIndex = muxer.addTrack(format)
        }


        if (!isMuxerStart) {
            startMuxer()
        }
        extractor.selectTrack(muxerItemData.trackIndex)
        while (true) {
            buffer.clear()
            val sampleSize = extractor.readSampleData(buffer, 0)
            if (sampleSize < 0) {
                break
            }
            bufferInfo.size = sampleSize
            bufferInfo.presentationTimeUs = extractor.sampleTime
            muxer.writeSampleData(trackIndex, buffer, bufferInfo)
            extractor.advance()
        }
        onDone.invoke()
    }

    private fun createAudioOutputFormat(inputFormat: MediaFormat): MediaFormat {
        if (inputFormat.getString(MediaFormat.KEY_MIME) == MediaFormat.MIMETYPE_AUDIO_AAC) {
            return inputFormat
        } else {
            val outputFormat = MediaFormat().apply {
                setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC)
                setInteger(
                    MediaFormat.KEY_AAC_PROFILE,
                    MediaCodecInfo.CodecProfileLevel.AACObjectELD
                )
                setInteger(
                    MediaFormat.KEY_SAMPLE_RATE,
                    inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                )
                setInteger(MediaFormat.KEY_BIT_RATE, OUTPUT_BIT_RATE)
                setInteger(
                    MediaFormat.KEY_CHANNEL_COUNT,
                    inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                )
            }

            return outputFormat
        }
    }

    private fun startMuxer() {
        muxer.start()
        isMuxerStart = true
    }

    private fun stopMuxer() {
        muxer.stop()
        muxer.release()
        isMuxerStart = false
    }


    override fun writeSampleData(
        trackType: TrackType,
        byteBuf: ByteBuffer,
        bufferInfo: MediaCodec.BufferInfo
    ) {
        TODO("Not yet implemented")
    }

    override fun setOutputFormat(trackType: TrackType, format: MediaFormat) {
        TODO("Not yet implemented")
    }

    class Builder {
        var outputPath: String = ""
            private set
        var muxerItems: List<MuxerItemData> = emptyList()
            private set

        fun setOutputPath(path: String) = apply { outputPath = path }
        fun setListItemToMerge(muxerItems: List<MuxerItemData>) =
            apply { this.muxerItems = muxerItems }

        fun build(): AudioMuxer {
            assert(outputPath.isNotEmpty()) { "Output path must not be empty" }
            assert(muxerItems.isNotEmpty()) { "List item to merge must not be empty" }
            return AudioMuxer(this)
        }
    }

}