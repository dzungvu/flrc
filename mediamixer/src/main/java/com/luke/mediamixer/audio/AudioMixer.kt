package com.luke.mediamixer.audio

import android.content.Context
import android.net.Uri
import com.luke.mediamixer.audio.helper.AudioExtractorHelper
import com.luke.mediamixer.audio.helper.AudioMuxer
import com.luke.mediamixer.common.model.MuxerItemData

class AudioMixer(private val context: Context) {

    private val listUri: MutableList<Uri> = mutableListOf()
    private val listMuxerItemData: MutableList<MuxerItemData> = mutableListOf()

    private var outputPath: String = ""

    fun addMediaUri(uri: String) {
        listUri.add(Uri.parse(uri))
    }

    fun addMediaUri(uri: Uri) {
        listUri.add(uri)
    }

    fun clearMediaUri() {
        listUri.clear()
    }

    fun setOutputPath(outputPath: String) {
        this.outputPath = outputPath
    }

    fun stop() {
        //TODO: Force stop the mixer
    }

    fun prepare(onDone: () -> Unit) {
        listUri.forEach { uri ->
            val audioExtractorHelper = AudioExtractorHelper(context = context, audioFileUri = uri)
            val audioTrackIndex = audioExtractorHelper.getAudioTrackIndex()
            val extractor = audioExtractorHelper.getExtractor()
            if (audioTrackIndex != -1 && extractor != null) {
                listMuxerItemData.add(
                    MuxerItemData(
                        mediaFormat = extractor.getTrackFormat(audioTrackIndex),
                        trackIndex = audioTrackIndex,
                        extractor = extractor
                    )
                )
            }

        }
        onDone.invoke()
    }

    fun mix() {
        val audioMuxer = AudioMuxer.Builder()
            .setOutputPath(outputPath)
            .setListItemToMerge(listMuxerItemData)
            .build()

        audioMuxer.mix()
    }
}