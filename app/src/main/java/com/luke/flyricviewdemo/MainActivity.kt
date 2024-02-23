package com.luke.flyricviewdemo

import android.animation.ValueAnimator
import android.os.Bundle
import android.util.Log
import android.widget.Button
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
        val lyricView = findViewById<FLyricUIView>(R.id.f_lyric_ui_view)
        val btnStart = findViewById<Button>(R.id.btn_start)

        lyricView.postDelayed({
            lyricView.setLyricData(R.raw.count)
        }, 1000)

        btnStart.setOnClickListener {
            lyricView.startKaraokeAnimation(34_000)
        }
    }

    private fun bindData() {


    }
}