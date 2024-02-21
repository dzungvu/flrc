package com.luke.flyricviewdemo

import android.animation.ValueAnimator
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.luke.flyricparser.FLRCParser
import com.luke.flyricparser.models.ArrayLyric
import com.luke.flyricparser.models.EnhancedLyric
import com.luke.flyricui.FLyricUIView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindUI()
        bindData()
    }

    private fun bindUI() {
        val textContent = getString(R.string.lorem_text_2)
        val lyricView = findViewById<FLyricUIView>(R.id.f_lyric_ui_view)

        lyricView.postDelayed({
            val textWidth = lyricView.setText(textContent)
            startKaraokeAnimation(lyricView, textWidth)
        }, 1000)
    }

    private fun bindData() {

        resources.openRawResource(R.raw.sample_en).use {
            val flrcParser = FLRCParser.Builder()
                .setFileDataType(FLRCParser.FileDataType.ELRC)
                .build()
            val lyricData = flrcParser.parseSource(it)

            lyricData.metaData?.let { metaData ->
                when (metaData.dataType) {
                    FLRCParser.FileDataType.LRC -> {
                        val lyric = lyricData.lyric as ArrayLyric
                        Log.d("MainActivity", "Lyric: $lyric")
                        Log.d("MainActivity", "Lyric Size: ${lyric.size()}")
                    }

                    FLRCParser.FileDataType.ELRC -> {
                        val lyric = lyricData.lyric as EnhancedLyric
                        Log.d("MainActivity", "Lyric: $lyric")
                        Log.d("MainActivity", "Lyric Size: ${lyric.size()}")
                    }

                    FLRCParser.FileDataType.AUTO_DETECT -> {
                        val lyric = lyricData.lyric as ArrayLyric
                        Log.d("MainActivity", "Lyric: $lyric")
                    }

                    null -> {
                        Log.d("MainActivity", "Lyric: null")
                    }
                }
            }
        }
    }

    private fun startKaraokeAnimation(lyricView: FLyricUIView, textWidth: Float) {
        val animator = ValueAnimator.ofFloat(0f, textWidth)
        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Float
            lyricView.setHighlightEnd(animatedValue)
        }
        animator.duration = 5000
        animator.start()
    }
}