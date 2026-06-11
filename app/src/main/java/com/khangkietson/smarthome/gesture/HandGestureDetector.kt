package com.khangkietson.smarthome.gesture

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult

class HandGestureDetector(
    private val context: Context,
    private val listener: GestureDetectorListener
) {
    private var gestureRecognizer: GestureRecognizer? = null

    interface GestureDetectorListener {
        fun onError(error: String)
        fun onResults(result: GestureRecognizerResult, mpImage: MPImage)
    }

    init {
        setupGestureRecognizer()
    }

    private fun setupGestureRecognizer() {
        val baseOptionsBuilder = BaseOptions.builder()
            .setModelAssetPath("gesture_recognizer.task")

        try {
            val optionsBuilder = GestureRecognizer.GestureRecognizerOptions.builder()
                .setBaseOptions(baseOptionsBuilder.build())
                .setMinHandDetectionConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setMinHandPresenceConfidence(0.5f)
                .setNumHands(2)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener { result: GestureRecognizerResult, image: MPImage ->
                    listener.onResults(result, image)
                }
                .setErrorListener { error: RuntimeException ->
                    listener.onError(error.message ?: "Unknown MediaPipe error")
                }

            val options = optionsBuilder.build()
            gestureRecognizer = GestureRecognizer.createFromOptions(context, options)
        } catch (e: Exception) {
            listener.onError("Failed to initialize Gesture Recognizer: ${e.message}")
            Log.e("HandGestureDetector", "MediaPipe init error", e)
        }
    }

    fun detectLiveStream(imageProxy: ImageProxy) {
        val gestureRecognizer = this.gestureRecognizer ?: return
        
        val bitmap = imageProxyToBitmap(imageProxy) ?: return
        val mpImage = BitmapImageBuilder(bitmap).build()
        
        val frameTime = SystemClock.uptimeMillis()
        try {
            gestureRecognizer.recognizeAsync(mpImage, frameTime)
        } catch (e: Exception) {
            Log.e("HandGestureDetector", "MediaPipe detection error", e)
        }
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        val bitmap = image.toBitmap()
        val rotationDegrees = image.imageInfo.rotationDegrees
        
        val matrix = android.graphics.Matrix()
        if (rotationDegrees != 0) {
            matrix.postRotate(rotationDegrees.toFloat())
        }
        // Mirroring the image so that left/right in preview matches front camera mirroring
        matrix.postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
        
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun close() {
        gestureRecognizer?.close()
        gestureRecognizer = null
    }
}
