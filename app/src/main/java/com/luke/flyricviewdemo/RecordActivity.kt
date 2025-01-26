package com.luke.flyricviewdemo

import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.luke.flyricviewdemo.fragments.PlayDualFragment
import com.luke.mediamixer2.audio_mixer.audio_container.AudioMixerWithAudioContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record)

        bindUI()
    }

    private fun bindUI() {
        val newFragment = PlayDualFragment()
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fl_record_activity, newFragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }
}