package com.luke.flyricviewdemo

import android.media.MediaCodecList
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log

class ImageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)

        bindInfo()
    }

    private fun bindInfo() {
        Log.d("ImageActivity", "isHEVCSupported: ${isHEVCSupported()}")
    }

    fun isHEVCSupported(): Boolean {
        val mimeType = "video/hevc"
        val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)

        for (codecInfo in codecList.codecInfos) {
            if (!codecInfo.isEncoder) {
                val types = codecInfo.supportedTypes
                if (types != null) {
                    for (type in types) {
                        if (type.equals(mimeType, ignoreCase = true)) {
                            return true
                        }
                    }
                }
            }
        }
        return false
    }
}