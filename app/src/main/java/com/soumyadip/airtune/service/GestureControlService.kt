package com.soumyadip.airtune.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView // KEPT: We need this for CameraX preview
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.soumyadip.airtune.R
import com.soumyadip.airtune.data.app_data.AppPreferences
import com.soumyadip.airtune.data.app_data.PrefManager
import com.soumyadip.airtune.feature.GestureTaskPerformerImpl
import com.soumyadip.airtune.util.GestureRecognizerHelper
import java.util.concurrent.Executors

class GestureControlService : LifecycleService(), GestureRecognizerHelper.GestureRecognizerListener {

    private lateinit var gestureRecognizerHelper: GestureRecognizerHelper
    private lateinit var audioManager: AudioManager
    private lateinit var windowManager: WindowManager
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null

    // CHANGED: From ComposeView to PreviewView
    private var cameraPreviewView: PreviewView? = null


    private lateinit var taskPerformer: GestureTaskPerformerImpl

    override fun onCreate() {
        super.onCreate()

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        taskPerformer = GestureTaskPerformerImpl(this)

        setupGestureRecognizer()
        startCameraWithPreview()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        startForegroundService()
        return START_STICKY
    }

    private fun startForegroundService() {

        val channelId = "gesture_control_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Gesture Control", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Gesture Control Active")
            .setContentText("Using front camera for gesture detection.")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use a valid drawable
            .build()

        startForeground(1, notification)

    }

    private fun setupGestureRecognizer() {

        gestureRecognizerHelper = GestureRecognizerHelper(
            context = this,
            runningMode = RunningMode.LIVE_STREAM,
            gestureRecognizerListener = this
        )

    }

    private fun startCameraWithPreview() {


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {

            return
        }

        val params = WindowManager.LayoutParams(
            1, 1, // Minimal size (1x1 pixels)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.START or Gravity.TOP

        // *** CHANGED: Create a plain PreviewView directly, NO ComposeView ***
        cameraPreviewView = PreviewView(this).apply {
            // No need for setViewTreeLifecycleOwner/setViewTreeSavedStateRegistryOwner here,
            // as PreviewView is a standard Android View and doesn't have Compose's requirements.
            // CameraX will bind to the service's Lifecycle.
        }

        windowManager.addView(cameraPreviewView, params)

        // *** MOVED CAMERA X BINDING LOGIC HERE from the removed Composable ***
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val currentCameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                this.cameraProvider = currentCameraProvider // Store reference for unbinding


                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build()


                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()


                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->

                    gestureRecognizerHelper.recognizeLiveStream(imageProxy)
                    imageProxy.close()
                }


                currentCameraProvider.unbindAll()

                // Create a Preview use case and set its SurfaceProvider
                val previewUseCase = androidx.camera.core.Preview.Builder()
                    .build()
                    .also {
                        // Crucially, connect the Preview use case to the surface of our PreviewView
                        it.setSurfaceProvider(cameraPreviewView?.surfaceProvider)
                    }

                // Bind both Preview and ImageAnalysis to the service's lifecycle
                currentCameraProvider.bindToLifecycle(
                    this, // The LifecycleService itself is the LifecycleOwner
                    cameraSelector,
                    previewUseCase, // Bind the Preview use case
                    imageAnalysis
                )


            } catch (exc: Exception) {

                onError("Camera initialization failed: ${exc.message}")
            }
        }, ContextCompat.getMainExecutor(this)) // Execute on the main thread
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResults(resultBundle: GestureRecognizerHelper.ResultBundle) {
        // ... (your existing onResults logic remains unchanged)
        //Log.d(TAG, "onResults received. Inference Time: ${resultBundle.inferenceTime}ms, Results Count: ${resultBundle.results.size}")

        resultBundle.results.firstOrNull()?.let { result ->
            if (result.gestures().isNotEmpty() && result.gestures().first().isNotEmpty()) {
                val topGesture = result.gestures().first().first()

                //Log.d(TAG, "Detected gesture: ${topGesture.categoryName()}, Score: ${topGesture.score()}")

                val category = topGesture.categoryName()
                val featureKey = AppPreferences.GESTURE_TO_FEATURE_KEY_MAP[category]

                if(featureKey != null){
                    val isEnabled = AppPreferences.isFeatureEnabled(this, featureKey, false)

                    if(isEnabled){
                        when(featureKey){
                            PrefManager.KEY_VOLUME_CONTROL_ENABLED -> taskPerformer.performVolumeTask(result)
                            PrefManager.KEY_BRIGHTNESS_CONTROL_ENABLED -> taskPerformer.performBrightnessTask(this@GestureControlService,result)
                            PrefManager.KEY_MEDIA_CONTROL_ENABLED -> taskPerformer.performMediaTask(this@GestureControlService,result)

                            else ->{}
                        }
                    } else
                    {

                    }
                }
                else{

                }
            } else {

            }
        }
    }

    override fun onError(error: String, errorCode: Int) {

    }

    override fun onDestroy() {
        super.onDestroy()

        cameraExecutor.shutdown()
        cameraProvider?.unbindAll() // Unbind all CameraX use cases

        // CHANGED: Remove the plain PreviewView (not ComposeView)
        if (cameraPreviewView != null) {
            windowManager.removeView(cameraPreviewView)
            cameraPreviewView = null

        }
        gestureRecognizerHelper.clearGestureRecognizer()

    }
}