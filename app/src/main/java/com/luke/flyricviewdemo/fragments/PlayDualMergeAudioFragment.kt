package com.luke.flyricviewdemo.fragments

import android.Manifest
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.luke.flyricviewdemo.R
import com.luke.flyricviewdemo.databinding.PlayDualMergeAudioFragmentBinding
import com.luke.mediamixer2.audio_mixer.audio_container.AudioMixerWithAudioContainer
import com.luke.mediamixer2.video_container.AudioMixerWithVideoContainer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.LinkedList
import java.util.Locale
import java.util.Queue


class PlayDualMergeAudioFragment private constructor(): Fragment() {

    companion object {
        const val RECORD_AUDIO_PERMISSION_REQUEST_CODE = 100
        const val RECORD_VIDEO_PERMISSION_REQUEST_CODE = 101

        const val RECORD_FILE_PATH = "record_file_path"
        const val RECORD_FILE_TYPE = "record_file_type"

        fun newInstance(recordFilePath: String, recordFileType: String): PlayDualMergeAudioFragment {
            val fragment = PlayDualMergeAudioFragment()
            val args = bundleOf(
                RECORD_FILE_PATH to recordFilePath,
                RECORD_FILE_TYPE to recordFileType
            )
            fragment.arguments = args
            return fragment
        }
    }
    private var _binding: PlayDualMergeAudioFragmentBinding? = null
    private val binding get() = _binding!!
    private val mapRequestPermissionHandle: HashMap<Int, Queue<Pair<() -> Unit, () -> Unit>>> =
        HashMap()

    private var audioMixerWithVideoContainer: AudioMixerWithVideoContainer? = null
    private var audioMixerWithAudioContainer: AudioMixerWithAudioContainer? = null

    private val fileBeat by lazy {
        File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS), "beat.mp3")
    }


    private var title: String = ""

    private val TAG = "tam-playDual"
    val mediaFile =
        "https://github.com/dzungvu/srtRepo/raw/master/hello-viet-nam-xin-chao-viet-nam-karaoke-beat-lyric-instrumental-nhac-goc-chuan-99.mp3"

    // declare the dialog as a member field of your activity
    var mProgressDialog: ProgressDialog? = null
    lateinit var recordFilePath: String
    lateinit var recordFileType: String
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PlayDualMergeAudioFragmentBinding.inflate(inflater, container, false)
        arguments?.let {
            recordFilePath = it.getString(RECORD_FILE_PATH) ?: ""
            recordFileType = it.getString(RECORD_FILE_TYPE) ?: ""
        } ?: kotlin.run {
            recordFilePath = ""
            recordFileType = ""
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindData()
        bindComponent()
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }



    fun bindComponent() {
        // instantiate it within the onCreate method
        mProgressDialog = ProgressDialog(requireContext()).apply {
            setMessage("Download beat");
            isIndeterminate = true;
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            setCancelable(true);

        }

        activity?.contentResolver?.let {
            audioMixerWithVideoContainer = AudioMixerWithVideoContainer(it)
            audioMixerWithAudioContainer = AudioMixerWithAudioContainer(it)

        }

        checkPemissionDownload()

    }

    fun bindData() {
        DownloadTask(requireContext(), mediaFile).execute()
    }


    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            RECORD_AUDIO_PERMISSION_REQUEST_CODE,
            RECORD_VIDEO_PERMISSION_REQUEST_CODE -> {
                if (checkPermissionResult(grantResults)) {
                    // Permission is granted. Continue the action or workflow
                    // in your app.
                    val queueRequestHandle = mapRequestPermissionHandle[requestCode]
                    if (!queueRequestHandle.isNullOrEmpty()) {
                        var requestHandlePair = queueRequestHandle.poll()
                        while (requestHandlePair != null) {
                            requestHandlePair.first.invoke()
                            requestHandlePair = queueRequestHandle.poll()
                        }
                    }
                } else {

                    val queueRequestHandle = mapRequestPermissionHandle[requestCode]
                    if (!queueRequestHandle.isNullOrEmpty()) {
                        var requestHandlePair = queueRequestHandle.poll()
                        while (requestHandlePair != null) {
                            requestHandlePair.second.invoke()
                            requestHandlePair = queueRequestHandle.poll()
                        }
                    }

                    // Phase 2
                    // Explain to the user that the feature is unavailable because
                    // the feature requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.

//                    showWarningPermissionDenied(title = getString(R.string.mini_app_warning_dialog_no_permission_read_contact)) {
//                        checkPermissionGetContacts("")
//                    }
                }
                return

            }

        }
    }

    private fun checkPemissionDownload() {
        val permissions = listOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE

        )
        return checkPermissions(
            listPermissions = permissions,
            requestCode = RECORD_AUDIO_PERMISSION_REQUEST_CODE,
            onGranted = {

            },
            onDenied = {
                showWarningPermissionDenied(title = getString(R.string.play_dual_warning_dialog_no_permission_record_audio)) {
                    showSetting()
                }

            }
        )

    }


    private fun mixSound(record: File, beat: File) {
        try {
            val audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                44100,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                44100,
                AudioTrack.MODE_STREAM
            )

            mProgressDialog?.show()
            mProgressDialog?.setMessage("Merge file. not show progress")

            val in1 = FileInputStream(record)
            val in2 = FileInputStream(beat)
            var arrayMusic1: ByteArray = createMusicArray(in1) ?: ByteArray(in1.available())
            in1.close()
            var arrayMusic2: ByteArray = createMusicArray(in2) ?: ByteArray(in2.available())
            in2.close()
            val output = ByteArray(arrayMusic1.size)
            audioTrack.play()
            for (i in output.indices) {
                val samplef1 = arrayMusic1[i] / 128.0f
                val samplef2 = arrayMusic2[i] / 128.0f
                var mixed = samplef1 + samplef2

                // reduce the volume a bit:
                mixed *= 0.8.toFloat()
                // hard clipping
                if (mixed > 1.0f) mixed = 1.0f
                if (mixed < -1.0f) mixed = -1.0f
                val outputSample = (mixed * 128.0f).toInt().toByte()
                output[i] = outputSample
            }
            audioTrack.write(output, 0, output.size)
            convertByteToFile(output)
        } catch(e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                context,
                "Merge error: ${e.printStackTrace()}",
                Toast.LENGTH_LONG
            ).show()

        } finally {
            Toast.makeText(context, "Merge completed", Toast.LENGTH_SHORT).show()
            mProgressDialog?.dismiss()

        }

    }

    @Throws(IOException::class)
    fun createMusicArray(inputStream: InputStream): ByteArray? {
        val baos = ByteArrayOutputStream()
        val buff = ByteArray(10240)
        var i = Int.MAX_VALUE
        while (inputStream.read(buff, 0, buff.size).also { i = it } > 0) {
            baos.write(buff, 0, i)
        }
        return baos.toByteArray() // be sure to close InputStream in calling function
    }

    @Throws(FileNotFoundException::class)
    fun convertByteToFile(fileBytes: ByteArray?) {
        val time = SimpleDateFormat("dd-MM-yyyy-hh_mm_ss", Locale.getDefault()).format(Date())

        val bos =
            BufferedOutputStream(FileOutputStream(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS).path + "/merge_audio_and_beat_$time.mp3"))
        try {
            bos.write(fileBytes)
            bos.flush()
            bos.close()
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } finally {
            Toast.makeText(context, "Write completed", Toast.LENGTH_SHORT).show()
            mProgressDialog?.dismiss()

        }
    }


    // usually, subclasses of AsyncTask are declared inside the activity class.
    // that way, you can easily modify the UI thread from here
    inner class DownloadTask(private val context: Context, private val url: String) :
        AsyncTask<String?, Int, String>() {

        private var mWakeLock: PowerManager.WakeLock? = null

        private var filePath: String? = null


        @Override
        override fun doInBackground(vararg params: String?): String? {
            var input: InputStream? = null
            var output: OutputStream? = null
            var  connection: HttpURLConnection? = null
            try {
                val url =  URL(url)
                connection = url.openConnection() as HttpURLConnection
                connection.connect()

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP ${connection.responseCode} ${connection.responseMessage}"
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                val fileLength = connection.contentLength

                // download the file
                input = connection.inputStream
                val time = SimpleDateFormat("dd-MM-yyyy-hh_mm_ss", Locale.getDefault()).format(Date())

                val file = File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), "beat_$time.mp3")
                output = FileOutputStream (file)
                filePath = file.absolutePath
                val data  = ByteArray(4096)
                var total = 0
                var count: Int = input.read(data)
                while (count != -1) {
                    // allow canceling with back button
                    if (isCancelled) {
                        input.close()
                        return null
                    }
                    total += count
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((total * 100) / fileLength)
                    output.write(data, 0, count)
                    count = input.read(data)
                }
            } catch (e: Exception ) {
                return e.toString()
            } finally {
                try {
                    output?.close()
                    input?.close()
                } catch ( ignored: IOException) {
                }

                connection?.disconnect()
            }
            return null
        }


        override fun onPreExecute() {
            super.onPreExecute()
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            mWakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                javaClass.name
            )
            mWakeLock?.acquire()
            mProgressDialog?.show()
        }

        override fun onProgressUpdate(vararg progress: Int?) {
            super.onProgressUpdate(*progress)
            // if we get here, length is known, now set indeterminate to false
            mProgressDialog?.let {
                it.isIndeterminate = false
                it.max = 100
                it.progress = progress?.get(0) ?: 0
            }

        }

        override fun onPostExecute(result: String?) {
            mWakeLock!!.release()
            mProgressDialog?.dismiss()
            if (result != null) Toast.makeText(
                context,
                "Download error: $result",
                Toast.LENGTH_LONG
            ).show() else Toast.makeText(context, "File downloaded", Toast.LENGTH_SHORT).show()

            val time = SimpleDateFormat("dd-MM-yyyy-hh_mm_ss", Locale.getDefault()).format(Date())
//            val mergedFile = File(Environment.getExternalStoragePublicDirectory(
//                Environment.DIRECTORY_DOWNLOADS), "merge_audio_and_beat_$time.mp3")
////            mergeSongs(mergedFile, File(safeArgs.recordFilePath), File(filePath))
//            mixSound(File(safeArgs.recordFilePath), File(filePath))

            lifecycleScope.launch(Dispatchers.Main) {
                if(recordFileType == "audio") {
                    val output =
                        withContext(Dispatchers.Default) {
                            audioMixerWithAudioContainer?.mix("mix_record_audio_with_audio_sample_$time.mp4", File(recordFilePath).toUri() , fileBeat.toUri())
                            audioMixerWithAudioContainer?.mix("mix_record_audio_with_beat_$time.mp4", File(recordFilePath).toUri() , File(filePath).toUri())

                        }
                } else {

                    val output =
                        withContext(Dispatchers.Default) {
                            audioMixerWithVideoContainer?.mix("mix_record_video_with_audio_sample_$time.mp4", File(recordFilePath).toUri() , fileBeat.toUri())
                            audioMixerWithVideoContainer?.mix("mix_record_video_with_beat_$time.mp4", File(recordFilePath).toUri() , File(filePath).toUri())

                        }
                }
                Toast.makeText(context, "Mix completed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // region check permission
    private fun checkPermissions(
        listPermissions: List<String>,
        requestCode: Int,
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ) {
        val listPermissionsNeeded: MutableList<String> = ArrayList()
        for (permission in listPermissions) {
            if (ContextCompat.checkSelfPermission(
                    requireActivity(),
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                listPermissionsNeeded.add(permission)
            }
        }

        when {
            listPermissionsNeeded.isEmpty() -> {
                // if granted, process and return data
                onGranted()
            }

            shouldShowRequestPermissionRationale(listPermissions[0]) -> {
                // In an educational UI, explain to the user why your app requires this
                // permission for a specific feature to behave as expected, and what
                // features are disabled if it's declined. In this UI, include a
                // "cancel" or "no thanks" button that lets the user continue
                // using your app without granting the permission.
//                showNoPermissionNotify()
                val queueRequestPermissionHandle =
                    if (mapRequestPermissionHandle[requestCode] != null) {
                        mapRequestPermissionHandle[requestCode]!!
                    } else {
                        LinkedList()
                    }

                queueRequestPermissionHandle.add(Pair(onGranted, onDenied))
                mapRequestPermissionHandle[requestCode] = queueRequestPermissionHandle
                requestPermissions(listPermissions.toTypedArray(), requestCode)

//                return onDenied()
            }

            else -> {
                // if have not granted, request permission from user while return blocking thread for html
                val queueRequestPermissionHandle =
                    if (mapRequestPermissionHandle[requestCode] != null) {
                        mapRequestPermissionHandle[requestCode]!!
                    } else {
                        LinkedList()
                    }
                queueRequestPermissionHandle.add(Pair(onGranted, onDenied))
                mapRequestPermissionHandle[requestCode] = queueRequestPermissionHandle
                requestPermissions(listPermissions.toTypedArray(), requestCode)

            }

        }
    }


    private fun showWarningPermissionDenied(title: String, onConfirm: () -> Unit) {
        Log.e(TAG, "showWarningPermissionDenied: ")
    }


    private fun showSetting() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", requireActivity().packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    fun checkPermissionResult(grantsResult: IntArray): Boolean {
        if (grantsResult.isNotEmpty()) {
            var allGranted = true
            for (result in grantsResult) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false
                    break
                }
            }
            return allGranted

        } else {
            return false
        }
    }


    // endregion check permission


}