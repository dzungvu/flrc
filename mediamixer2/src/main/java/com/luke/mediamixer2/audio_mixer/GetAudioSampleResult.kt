package com.luke.mediamixer2.audio_mixer

sealed class GetAudioSampleResult {
    object Pending : GetAudioSampleResult()
    object Eos : GetAudioSampleResult()
    data class Ready(val sample: AudioSample) : GetAudioSampleResult()
}