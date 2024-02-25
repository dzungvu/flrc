package com.luke.flyricviewdemo

import android.content.ContentResolver
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.luke.flyricui.FLyricUIView

class MainActivity : AppCompatActivity() {

    private val mediaPlayer = MediaPlayer()
    private val lyricView by lazy { findViewById<FLyricUIView>(R.id.f_lyric_ui_view) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindUI()
        bindData()
    }

    private fun bindUI() {
        val lyricView = findViewById<FLyricUIView>(R.id.f_lyric_ui_view)
        val btnStart = findViewById<Button>(R.id.btn_start)

        btnStart.setOnClickListener {
            lyricView.startKaraokeAnimation()
            mediaPlayer.start()
        }
    }

    private fun bindData() {
        prepareSongLyric()
        prepareMediaPlayer()
    }

    private fun prepareSongLyric() {
        lyricView.postDelayed({
            lyricView.setLyricData(R.raw.sample)
        }, 1000)
    }

    private fun prepareMediaPlayer() {
        val songResource = R.raw.mp3_earnedit
        val uriSong = Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(resources.getResourcePackageName(songResource))
            .appendPath(resources.getResourceTypeName(songResource))
            .appendPath(resources.getResourceEntryName(songResource))
            .build()
        mediaPlayer.setDataSource(this, uriSong)
        mediaPlayer.prepare()
    }
}