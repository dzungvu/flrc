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
    private val btnStart: Button by lazy { findViewById(R.id.btn_start) }
    private val btnSeekForward: Button by lazy { findViewById(R.id.btn_seek_forward) }
    private val btnSeekBackward: Button by lazy { findViewById(R.id.btn_seek_backward) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindUI()
        bindData()
    }

    private fun bindUI() {
        val lyricView = findViewById<FLyricUIView>(R.id.f_lyric_ui_view)

        btnStart.setOnClickListener {
            when (btnStart.text) {
                getString(R.string.start) -> {
                    startKaraoke()
                    btnStart.text = getString(R.string.pause)
                }

                getString(R.string.pause) -> {
                    pauseKaraoke()
                    btnStart.text = if (lyricView.isKaraokeAnimationRunning()) {
                        getString(R.string.resume)
                    } else {
                        getString(R.string.start)
                    }
                }

                getString(R.string.resume) -> {
                    resumeKaraoke()
                    btnStart.text = getString(R.string.pause)
                }

                else -> {
                    if (lyricView.isKaraokeAnimationRunning()) {
                        resumeKaraoke()
                    }
                }
            }
        }

        btnSeekForward.setOnClickListener {
            seekForward()
        }
        btnSeekBackward.setOnClickListener {
            seekBackward()
        }
    }

    private fun startKaraoke() {
        lyricView.startKaraokeAnimation()
        mediaPlayer.start()
    }

    private fun pauseKaraoke() {
        lyricView.pauseKaraokeAnimation()
        mediaPlayer.pause()
    }

    private fun resumeKaraoke() {
        lyricView.resumeKaraokeAnimation()
        mediaPlayer.start()
    }

    private fun seekForward() {
        mediaPlayer.seekTo(mediaPlayer.currentPosition + 5000)
        lyricView.seekAnimationToValue(mediaPlayer.currentPosition.toLong())
    }

    private fun seekBackward() {
        mediaPlayer.seekTo(mediaPlayer.currentPosition - 5000)
        lyricView.seekAnimationToValue(mediaPlayer.currentPosition.toLong())
    }

    private fun bindData() {
        prepareSongLyric()
        prepareMediaPlayer()
    }

    private fun prepareSongLyric() {
        lyricView.postDelayed({
            lyricView.setLyricData(R.raw.sample_en)
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
        mediaPlayer.setOnCompletionListener {
            lyricView.stopKaraokeAnimation()
            btnStart.text = getString(R.string.start)
        }
        mediaPlayer.prepare()
    }
}