package com.example.camera

import android.content.ContentValues
import android.content.Context
import android.media.MediaActionSound
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.MeteringPoint
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraManager(private val context: Context) {

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val mediaActionSound = MediaActionSound()

    var camera: Camera? = null
        private set
    var cameraControl: CameraControl? = null
        private set
    var cameraInfo: CameraInfo? = null
        private set

    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null

    var isRecordingVideo: Boolean = false
        private set

    var lensFacing: Int = CameraSelector.LENS_FACING_BACK
        private set

    init {
        try {
            mediaActionSound.load(MediaActionSound.SHUTTER_CLICK)
            mediaActionSound.load(MediaActionSound.START_VIDEO_RECORDING)
            mediaActionSound.load(MediaActionSound.STOP_VIDEO_RECORDING)
            mediaActionSound.load(MediaActionSound.FOCUS_COMPLETE)
        } catch (e: Exception) {
            Log.w("CameraManager", "Could not preload media sounds", e)
        }
    }

    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        targetLensFacing: Int = CameraSelector.LENS_FACING_BACK,
        onCameraBound: ((Camera) -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null
    ) {
        lensFacing = targetLensFacing
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                // Configure maximum quality high-resolution image capture
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .build()

                // Configure highest quality video recorder
                val qualitySelector = QualitySelector.from(
                    Quality.HIGHEST,
                    FallbackStrategy.higherQualityOrLowerThan(Quality.SD)
                )
                val recorder = Recorder.Builder()
                    .setQualitySelector(qualitySelector)
                    .setExecutor(cameraExecutor)
                    .build()
                videoCapture = VideoCapture.withOutput(recorder)

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                cameraProvider.unbindAll()

                val boundCamera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                    videoCapture
                )

                camera = boundCamera
                cameraControl = boundCamera.cameraControl
                cameraInfo = boundCamera.cameraInfo

                onCameraBound?.invoke(boundCamera)
            } catch (exc: Throwable) {
                Log.e("CameraManager", "Use case binding failed", exc)
                onError?.invoke(exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun flipCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onCameraBound: ((Camera) -> Unit)? = null
    ) {
        val newLensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        startCamera(lifecycleOwner, previewView, newLensFacing, onCameraBound)
        vibrate(30)
    }

    fun setZoomRatio(ratio: Float) {
        val info = cameraInfo ?: return
        val currentZoomState = info.zoomState.value ?: return
        val targetRatio = ratio.coerceIn(currentZoomState.minZoomRatio, currentZoomState.maxZoomRatio)
        cameraControl?.setZoomRatio(targetRatio)
    }

    fun focusOnPoint(meteringPoint: MeteringPoint) {
        try {
            val action = FocusMeteringAction.Builder(meteringPoint, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                .setAutoCancelDuration(2, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            cameraControl?.startFocusAndMetering(action)
        } catch (e: Exception) {
            Log.w("CameraManager", "Focus failed", e)
        }
    }

    fun takePhoto(
        onEnhanceStatus: ((String) -> Unit)? = null,
        onImageSaved: (Uri, Boolean) -> Unit,
        onError: (ImageCaptureException) -> Unit
    ) {
        val capture = imageCapture ?: run {
            Log.e("CameraManager", "ImageCapture is not initialized")
            return
        }

        // Haptic feedback & shutter sound
        vibrate(40)
        try {
            mediaActionSound.play(MediaActionSound.SHUTTER_CLICK)
        } catch (e: Exception) {
            Log.w("CameraManager", "Sound playback failed", e)
        }

        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG_$name.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PureCamera")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        capture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = outputFileResults.savedUri ?: return

                    ContextCompat.getMainExecutor(context).execute {
                        onEnhanceStatus?.invoke("✨ Auto-Enhancing to iPhone 17 Pro Max...")
                    }

                    // Stage 1: Local iPhone 17 Pro Max Photonic Engine enhancement
                    val rawBitmap = IPhone17ProEnhancer.loadBitmapFromUri(context, savedUri)
                    val photonicEnhanced = if (rawBitmap != null) {
                        try {
                            val enhanced = IPhone17ProEnhancer.enhancePhotonicEngine(rawBitmap)
                            IPhone17ProEnhancer.saveBitmapToUri(context, savedUri, enhanced)
                            enhanced
                        } catch (e: Exception) {
                            Log.w("CameraManager", "Photonic engine pass failed", e)
                            rawBitmap
                        }
                    } else null

                    // Clear pending state in MediaStore
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        try {
                            val values = ContentValues().apply {
                                put(MediaStore.Images.Media.IS_PENDING, 0)
                            }
                            context.contentResolver.update(savedUri, values, null, null)
                        } catch (e: Exception) {
                            Log.w("CameraManager", "Failed to clear pending state", e)
                        }
                    }

                    // Notify UI of high quality enhancement
                    ContextCompat.getMainExecutor(context).execute {
                        onImageSaved(savedUri, false)
                    }

                    // Stage 2: Gemini AI Neural Enhancement if API key is active
                    if (photonicEnhanced != null && IPhone17ProEnhancer.hasValidGeminiApiKey()) {
                        ContextCompat.getMainExecutor(context).execute {
                            onEnhanceStatus?.invoke("✨ AI Neural Enhancing (iPhone 17 Pro Max)...")
                        }

                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            try {
                                val aiEnhanced = IPhone17ProEnhancer.enhanceWithGeminiAi(photonicEnhanced)
                                if (aiEnhanced != null) {
                                    IPhone17ProEnhancer.saveBitmapToUri(context, savedUri, aiEnhanced)
                                    ContextCompat.getMainExecutor(context).execute {
                                        onImageSaved(savedUri, true)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.w("CameraManager", "Gemini neural enhancement pass failed", e)
                            }
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraManager", "Photo capture failed: ${exception.message}", exception)
                    ContextCompat.getMainExecutor(context).execute {
                        onError(exception)
                    }
                }
            }
        )
    }

    fun startRecording(
        hasAudioPermission: Boolean,
        onEvent: (VideoRecordEvent) -> Unit
    ) {
        val videoCap = videoCapture ?: run {
            Log.e("CameraManager", "VideoCapture not initialized")
            return
        }

        if (isRecordingVideo) return

        vibrate(60)
        try {
            mediaActionSound.play(MediaActionSound.START_VIDEO_RECORDING)
        } catch (e: Exception) {
            Log.w("CameraManager", "Sound playback failed", e)
        }

        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "VID_$name.mp4")
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/PureCamera")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val mediaStoreOutput = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()

        val pendingRecording = videoCap.output.prepareRecording(context, mediaStoreOutput)
        if (hasAudioPermission) {
            try {
                pendingRecording.withAudioEnabled()
            } catch (se: SecurityException) {
                Log.w("CameraManager", "Audio permission missing for recording", se)
            }
        }

        isRecordingVideo = true

        activeRecording = pendingRecording.start(ContextCompat.getMainExecutor(context)) { event ->
            when (event) {
                is VideoRecordEvent.Start -> {
                    isRecordingVideo = true
                }
                is VideoRecordEvent.Finalize -> {
                    isRecordingVideo = false
                    if (!event.hasError()) {
                        val uri = event.outputResults.outputUri
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            try {
                                val values = ContentValues().apply {
                                    put(MediaStore.Video.Media.IS_PENDING, 0)
                                }
                                context.contentResolver.update(uri, values, null, null)
                            } catch (e: Exception) {
                                Log.w("CameraManager", "Failed to finalize video in MediaStore", e)
                            }
                        }
                    }
                    try {
                        mediaActionSound.play(MediaActionSound.STOP_VIDEO_RECORDING)
                    } catch (e: Exception) {
                        Log.w("CameraManager", "Sound playback failed", e)
                    }
                    vibrate(40)
                }
            }
            onEvent(event)
        }
    }

    fun stopRecording() {
        if (!isRecordingVideo) return
        activeRecording?.stop()
        activeRecording = null
        isRecordingVideo = false
    }

    private fun vibrate(durationMillis: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMillis, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(durationMillis)
            }
        } catch (e: Exception) {
            Log.w("CameraManager", "Vibrate failed", e)
        }
    }

    fun release() {
        try {
            activeRecording?.stop()
            activeRecording = null
            cameraExecutor.shutdown()
            mediaActionSound.release()
        } catch (e: Exception) {
            Log.w("CameraManager", "Release failed", e)
        }
    }
}
