package com.luke.flyricviewdemo.fragments

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Camera
import android.media.AudioManager
import android.media.CamcorderProfile
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.luke.flyricviewdemo.R
import com.luke.flyricviewdemo.databinding.PlayDualFragmentBinding
import com.luke.flyricviewdemo.util.getDisplayHeight
import com.luke.flyricviewdemo.util.getDisplayWidth
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.LinkedList
import java.util.Locale
import java.util.Queue
import java.util.concurrent.Semaphore


class PlayDualFragment: Fragment() {


    companion object {
        const val RECORD_AUDIO_PERMISSION_REQUEST_CODE = 100
        const val RECORD_VIDEO_PERMISSION_REQUEST_CODE = 101
    }

    private var _binding: PlayDualFragmentBinding? = null
    private val binding get() = _binding!!

    private var mediaRecorder: MediaRecorder? = null
    private var recorderInit = false
    private var recording = false
    private var currentFilePath: String? = null
    private var camera: Camera? = null


    private var title: String = ""

    private val TAG = "tam-playDual"
    private val mediaFile =
        "https://github.com/dzungvu/srtRepo/raw/master/hello-viet-nam-xin-chao-viet-nam-karaoke-beat-lyric-instrumental-nhac-goc-chuan-99.mp3"
    private var mediaPlayer: MediaPlayer? = null
    private var isBeatMediaPlaying = false
    private val mapRequestPermissionHandle: HashMap<Int, Queue<Pair<() -> Unit, () -> Unit>>> =
        HashMap()

    /**
     * A [Semaphore] to prevent the app from exiting before closing the camera.
     */
    private val mCameraOpenCloseLock: Semaphore = Semaphore(1)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PlayDualFragmentBinding.inflate(inflater, container, false)
        bindEvent()
        bindComponent()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStop() {
        super.onStop()
        stopBeatAudio()
        stopCamera()
        if (recording) {
            mediaRecorder?.stop()
            recording = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopBeatAudio()
        mediaPlayer?.release()
        if (recording) {
            mediaRecorder?.stop()
            recording = false
        }
        mediaRecorder?.release()
    }



    fun bindComponent() {
//        checkPermissionRecordAudio()
        checkPermissionRecordVideo()
        mediaPlayer = MediaPlayer()

    }

     fun bindEvent() {

        binding.surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
//                prepareRecorder(videoRecording = true)
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                stopBeatAudio()
                if (recording) {
                    mediaRecorder?.stop();
                    recording = false;
                }
                mediaRecorder?.release();
            }
        })

        binding.ibRecordVideo.setOnClickListener {
            if (recording) {
                stopBeatAudio()
                mediaRecorder?.stop();
                recording = false;


                val newFragment = PlayDualMergeAudioFragment.newInstance(
                    recordFilePath = currentFilePath ?: "",
                    recordFileType = "video"
                )
                val transaction = parentFragmentManager.beginTransaction()
                transaction.replace(R.id.fl_record_activity, newFragment)
                transaction.addToBackStack(null)
                transaction.commit()


                currentFilePath = null


                // Let's initRecorder so we can record again
//                initRecorder(videoRecording = true);
//                prepareRecorder(videoRecording = true);
            } else {
                checkPermissionRecordVideo(startAfterCheck = true)

//                if(mediaRecorder == null) {
//                    checkPermissionRecordVideo(startAfterCheck = true)
//                    return@setOnClickListener
//                } else {
//                    Timber.tag("tam-playDual").i("mediaRecorder star record video $mediaRecorder")
//                    recording = true
//                    mediaRecorder?.start();
//
//                }
            }
        }

        binding.ibRecordAudio.setOnClickListener {
            if (recording) {
                stopBeatAudio()
                mediaRecorder?.stop();
                recording = false;

                val newFragment = PlayDualMergeAudioFragment.newInstance(
                    recordFilePath = currentFilePath ?: "",
                    recordFileType = "audio"
                )
                val transaction = parentFragmentManager.beginTransaction()
                transaction.replace(R.id.fl_record_activity, newFragment)
                transaction.addToBackStack(null)
                transaction.commit()

                currentFilePath = null
                // Let's initRecorder so we can record again
//                initRecorder(videoRecording = false);
//                prepareRecorder(videoRecording = false);
            } else {
                checkPermissionRecordAudio(startAfterCheck = true)

//                if(mediaRecorder == null) {
//                    checkPermissionRecordAudio(startAfterCheck = true)
//                    return@setOnClickListener
//                } else {
//                    Timber.tag("tam-playDual").i("mediaRecorder star record audio $mediaRecorder")
//                    recording = true
//                    mediaRecorder?.start();
//                }
            }
        }

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

    private fun checkPermissionRecordAudio(startAfterCheck: Boolean = false) {
        val permissions = listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE

        )
        return checkPermissions(
            listPermissions = permissions,
            requestCode = RECORD_AUDIO_PERMISSION_REQUEST_CODE,
            onGranted = {
                if (startAfterCheck) {
                    initRecorder(videoRecording = false)
                    prepareRecorder(videoRecording = false);

                    recording = true
                    playBeatAudio(mediaFile)
                    mediaRecorder?.start();
                }

            },
            onDenied = {
                showWarningPermissionDenied(title = getString(R.string.play_dual_warning_dialog_no_permission_record_audio)) {
                    showSetting()
                }

            }
        )

    }

    private fun checkPermissionRecordVideo(startAfterCheck: Boolean = false) {
        val permissions = listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE

        )
        return checkPermissions(
            listPermissions = permissions,
            requestCode = RECORD_VIDEO_PERMISSION_REQUEST_CODE,
            onGranted = {
                if (startAfterCheck) {
                    initRecorder(videoRecording = true)
                    prepareRecorder(videoRecording = true);

                    recording = true
                    playBeatAudio(mediaFile)
                    mediaRecorder?.start();
                }
            },
            onDenied = {
                showWarningPermissionDenied(title = getString(R.string.play_dual_warning_dialog_no_permission_record_video)) {
                    showSetting()
                }

            }
        )

    }

    private fun initRecorder(videoRecording: Boolean = true) {

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(requireContext())
        } else {
            MediaRecorder()
        }
        mediaRecorder?.let {
            val time = SimpleDateFormat("dd-MM-yyyy-hh_mm_ss", Locale.getDefault()).format(Date())
            if (videoRecording) {
                startCamera()
                val width = context?.getDisplayWidth() ?: 0
                val height = context?.getDisplayHeight() ?: 0
                it.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                it.setVideoSource(MediaRecorder.VideoSource.CAMERA)
                val cpHigh = CamcorderProfile
                    .get(CamcorderProfile.QUALITY_HIGH)
//                it.setProfile(cpHigh)
                val file = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "video_play_dual_$time.mp4"
                )
                it.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                it.setOutputFile(file.absolutePath)
                currentFilePath = file.absolutePath
//                it.setVideoSize(width, height )
                // audio encoder have to be called after setOutputFormat
                it.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                it.setAudioEncodingBitRate(128000)
                it.setAudioSamplingRate(48000)

//                it.setVideoEncodingBitRate(15000000)
                it.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
                it.setVideoEncodingBitRate(128000)
//                it.setVideoEncodingBitRate(1280*720)
                it.setVideoFrameRate(30)

//                // orientation
//                var orientation = 0
//                val info = Camera.CameraInfo()
//                Camera.getCameraInfo(0, info)
//                orientation = (orientation + 45) / 90 * 90
//                var rotation = 0
//                rotation = if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
//                    (info.orientation - orientation + 360) % 360
//                } else {  // back-facing camera
//                    (info.orientation + orientation) % 360
//                }
//                Camera.setRotation(rotation)
                it.setOrientationHint(90)
                it.setMaxDuration(50000) // 50 seconds

                it.setMaxFileSize(5000000) // Approximately 5 megabytes

            } else {
                it.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                val file = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "audio_play_dual_$time.3gp"
                )
                it.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                it.setOutputFile(file.absolutePath)
                currentFilePath = file.absolutePath
                // audio encoder have to be called after setOutputFormat
                it.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                it.setAudioEncodingBitRate(128000)
                it.setAudioSamplingRate(48000)
                it.setMaxDuration(50000) // 50 seconds

                it.setMaxFileSize(5000000) // Approximately 5 megabytes

            }

        }
    }

    private fun prepareRecorder(videoRecording: Boolean = true) {
        if (videoRecording) {

            mediaRecorder?.setPreviewDisplay(binding.surfaceView.holder.surface)
        }
        try {
            mediaRecorder?.prepare()
        } catch (e: IllegalStateException) {
            Toast.makeText(requireContext(), "prepareRecorder error", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        } catch (e: IOException) {
            Toast.makeText(requireContext(), "prepareRecorder error", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    // region camera

    private fun startCamera() {
        // Create an instance of Camera
        if (camera == null) {
            camera = try {
                Camera.open() // attempt to get a Camera instance
            } catch (e: Exception) {
                // Camera is not available (in use or does not exist)
                null // returns null if camera is unavailable
            }
        }
        camera?.let {
            it.stopPreview()
            it.setDisplayOrientation(90)
            it.startPreview()
            it.unlock();
            mediaRecorder?.setCamera(it);
        }
//        val width = context?.getDisplayWidth() ?: 0
//        val height = context?.getDisplayHeight() ?: 0
//
//
//        val manager = requireActivity().getSystemService(Context.CAMERA_SERVICE) as CameraManager
//        try {
//            Timber.tag(TAG).d("tryAcquire")
//            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
//                throw RuntimeException("Time out waiting to lock camera opening.")
//            }
//            /**
//             * default front camera will activate
//             */
//            val cameraId = manager.cameraIdList[0]
//            val characteristics = manager.getCameraCharacteristics(cameraId)
//            val map = characteristics
//                .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
//            val mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)
//            if (map == null) {
//                throw RuntimeException("Cannot get available preview/video sizes")
//            }
//            val mVideoSize =
//                chooseVideoSize(map.getOutputSizes<MediaRecorder>(MediaRecorder::class.java))
//            val mPreviewSize = chooseOptimalSize(
//                map.getOutputSizes<SurfaceTexture>(SurfaceTexture::class.java),
//                width, height, mVideoSize
//            )
//            val orientation = resources.configuration.orientation
////            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
////
////                binding.surfaceView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight())
////            } else {
////                mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth())
////            }
//            configureTransform(width, height)
//
//            manager.openCamera(cameraId, mStateCallback, null)
//        } catch (e: CameraAccessException) {
//            Log.e(TAG, "openCamera: Cannot access the camera.")
//        } catch (e: NullPointerException) {
//            Log.e(TAG, "Camera2API is not supported on the device.")
//        } catch (e: InterruptedException) {
//            throw RuntimeException("Interrupted while trying to lock camera opening.")
//        }

//        activity?.let {
//
//            val cameraProviderFuture = ProcessCameraProvider.getInstance(it)
//            cameraProviderFuture.addListener( {
//                // Used to bind the lifecycle of cameras to the lifecycle owner
//                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
//                // Preview
//                val preview = Preview.Builder()
//                    .build()
//                    .also { preview ->
//                        preview.setSurfaceProvider(binding.previewView.surfaceProvider)
//                    }
//
//                // Select back camera as a default
//                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
//
//                try {
//                    // Unbind use cases before rebinding
//                    cameraProvider.unbindAll()
//
//                    // Bind use cases to camera
//                    cameraProvider.bindToLifecycle(
//                        this, cameraSelector, preview)
//
//                } catch(exc: Exception) {
//                    Timber.tag("tam-playDual").e(exc, "Use case binding failed")
//                }
//
//            }, ContextCompat.getMainExecutor(it))
//
//
//        }

    }

    /**
     * close camera and release object
     */
    private fun closeCamera() {
        try {
            mCameraOpenCloseLock.acquire()
        } catch (e: InterruptedException) {
            throw java.lang.RuntimeException("Interrupted while trying to lock camera closing.")
        } finally {
            mCameraOpenCloseLock.release()
        }
    }

    /**
     * In this sample, we choose a video size with 3x4 for  aspect ratio. for more perfectness 720 as
     * well Also, we don't use sizes
     * larger than 1080p, since MediaRecorder cannot handle such a high-resolution video.
     *
     * @param choices The list of available sizes
     * @return The video size 1080p,720px
     */
    private fun chooseVideoSize(choices: Array<Size>): Size? {
        for (size in choices) {
            if (1920 == size.width && 1080 == size.height) {
                return size
            }
        }
        for (size in choices) {
            if (size.width == size.height * 4 / 3 && size.width <= 1080) {
                return size
            }
        }
        return choices[choices.size - 1]
    }

    /**
     * Given `choices` of `Size`s supported by a camera, chooses the smallest one whose
     * width and height are at least as large as the respective requested values, and whose aspect
     * ratio matches with the specified value.
     *
     * @param choices The list of sizes that the camera supports for the intended output class
     * @param width The minimum desired width
     * @param height The minimum desired height
     * @param aspectRatio The aspect ratio
     * @return The optimal `Size`, or an arbitrary one if none were big enough
     */
    private fun chooseOptimalSize(
        choices: Array<Size>,
        width: Int,
        height: Int,
        aspectRatio: Size?
    ): Size? {
        // Collect the supported resolutions that are at least as big as the preview Surface
        val bigEnough: MutableList<Size> = ArrayList()
        val w = aspectRatio?.width ?: 0
        val h = aspectRatio?.height ?: 0
        for (option in choices) {
            if (option.height == option.width * h / w && option.width >= width && option.height >= height) {
                bigEnough.add(option)
            }
        }
        // Pick the smallest of those, assuming we found any
        return if (bigEnough.size > 0) {
            Collections.min<Size>(bigEnough, CompareSizesByArea())
        } else {
            choices[0]
        }
    }

    private fun stopCamera() {
        camera?.stopPreview()
        camera?.release()
    }

    /**
     * Compares two `Size`s based on their areas.
     */
    internal class CompareSizesByArea : Comparator<Size?> {
        override fun compare(lhs: Size?, rhs: Size?): Int {
            // We cast here to ensure the multiplications won't overflow
            return java.lang.Long.signum(
                (lhs?.width?.toLong() ?: 0) * (lhs?.height ?: 0) -
                        (rhs?.width?.toLong() ?: 0) * (rhs?.height ?: 0)
            )
        }
    }

    // endregion camera

    // region beat
    private fun playBeatAudio(url: String) {
        if (!isBeatMediaPlaying) {
            isBeatMediaPlaying = true
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer()
            }
            try {
                mediaPlayer?.let {
                    it.setVolume(0.5F, 0.5F)
                    it.setAudioStreamType(AudioManager.STREAM_MUSIC)
                    it.setDataSource(url)
                    it.prepare()
                    it.start()
                }
            } catch (e: IOException) {
                Toast.makeText(requireContext(), "playBeatAudio error", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }

    }

    private fun stopBeatAudio() {
        if (isBeatMediaPlaying) {
            isBeatMediaPlaying = false
            mediaPlayer?.stop()
            mediaPlayer?.reset()

        }

    }

    // endregion beat
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
        Log.e(TAG, "showWarningPermissionDenied: $title")
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