package com.example.tactile_main

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

class HandLandmarkHelper(
    context: Context,
    modelName: String = "best_float32.tflite", // your detection model
    private val onAdjustedIndexTip: (Float, Float) -> Unit
) {
    private val tfliteHelper = TFLiteHelper(context, modelName)
    private val handLandmarker: HandLandmarker

    init {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("hand_landmarker.task")
            .build()

        val options = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumHands(1)
            .setResultListener { result, mpImage ->
                handleLandmarkResult(result, mpImage)
            }
            .build()

        handLandmarker = HandLandmarker.createFromOptions(context, options)
    }

    fun detect(bitmap: Bitmap) {
        val preprocessed = preprocess(bitmap)
        val inputBuffer = preprocessed.buffer
        val resizedBitmap = preprocessed.resizedBitmap

        // 1. Run object detection
        val detection = tfliteHelper.runInference(inputBuffer)

        // 2. Process detection
        val x = detection[0]
        val y = detection[1]
        val w = detection[2]
        val h = detection[3]
        val conf = detection[4]

        if (conf < 0.5f) {
            Log.w("HandLandmarkHelper", "Low confidence, skipping.")
            return
        }

        // 3. Calculate bounding box
        val boxLeft = ((x - w / 2f) * 640).coerceIn(0f, 639f)
        val boxTop = ((y - h / 2f) * 640).coerceIn(0f, 639f)
        val boxRight = ((x + w / 2f) * 640).coerceIn(0f, 640f)
        val boxBottom = ((y + h / 2f) * 640).coerceIn(0f, 640f)

        lastDetectionBox = floatArrayOf(boxLeft, boxTop, boxRight - boxLeft, boxBottom - boxTop)

        // 4. Run landmark detection using resized bitmap
        val mpImage = BitmapImageBuilder(resizedBitmap).build()
        handLandmarker.detectAsync(mpImage, SystemClock.uptimeMillis())
    }

    private var lastDetectionBox: FloatArray? = null

    private fun handleLandmarkResult(result: HandLandmarkerResult, mpImage: MPImage) {
        if (result.landmarks().isEmpty() || lastDetectionBox == null) return

        val landmarks = result.landmarks()[0]
        if (landmarks.size <= 8) return

        val indexTip = landmarks[8] // Index finger tip

        // Image size = 640x640
        val absoluteX = indexTip.x() * 640f
        val absoluteY = indexTip.y() * 640f

        val (boxLeft, boxTop, boxWidth, boxHeight) = lastDetectionBox!!

        // Adjust finger position relative to detection box
        val adjustedX = ((absoluteX - boxLeft) / boxWidth).coerceIn(0f, 1f)
        val adjustedY = ((absoluteY - boxTop) / boxHeight).coerceIn(0f, 1f)

        // 🔁 Unnormalize: convert back to full image coordinate system
        val unnormalizedX = boxLeft + (adjustedX * boxWidth)
        val unnormalizedY = boxTop + (adjustedY * boxHeight)

        Log.d("HandLandmarkHelper", "👆 Index finger absolute: ($absoluteX, $absoluteY)")
        Log.d("HandLandmarkHelper", "📦 Detection box: left=$boxLeft, top=$boxTop, w=$boxWidth, h=$boxHeight")
        Log.d("HandLandmarkHelper", "📍 Unnormalized index tip: ($unnormalizedX, $unnormalizedY)")

        onAdjustedIndexTip(unnormalizedX, unnormalizedY)
    }


    fun close() {
        handLandmarker.close()
        tfliteHelper.close()
    }
}
