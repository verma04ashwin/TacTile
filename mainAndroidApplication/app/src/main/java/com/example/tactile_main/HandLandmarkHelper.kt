package com.example.tactile_main

import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.example.tactile_main.handlandmarkerhelping.FrameSaver

class HandLandmarkHelper(
    private val context: Context,
    modelName: String = "best_float16.tflite",
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
            .setResultListener { result, _ ->
                handleLandmarkResult(result)
                handleLandmarkResult(result)
            }
            .build()

        handLandmarker = HandLandmarker.createFromOptions(context, options)
    }

    fun detect(bitmap: Bitmap) {
        val preprocessed = preprocess(bitmap)
        val inputBuffer = preprocessed.buffer
        val resizedBitmap = preprocessed.resizedBitmap // 640x640

        val detection = tfliteHelper.runInference(inputBuffer)
        val x = detection[0]
        val y = detection[1]
        val w = detection[2]
        val h = detection[3]
        val conf = detection[4]

        if (conf < 0.5f) {
            Log.w("HandLandmarkHelper", "Low confidence, skipping.")
            return
        }

        val boxLeft = ((x - w / 2f) * 640).toInt().coerceIn(0, 639)
        val boxTop = ((y - h / 2f) * 640).toInt().coerceIn(0, 639)
        val boxRight = ((x + w / 2f) * 640).toInt().coerceIn(0, 640)
        val boxBottom = ((y + h / 2f) * 640).toInt().coerceIn(0, 640)

        val padding = 50
        val availableLeft = boxLeft
        val availableTop = boxTop
        val availableRight = 640 - boxRight
        val availableBottom = 640 - boxBottom

        val padLeft = (padding - availableLeft).coerceAtLeast(0)
        val padTop = (padding - availableTop).coerceAtLeast(0)
        val padRight = (padding - availableRight).coerceAtLeast(0)
        val padBottom = (padding - availableBottom).coerceAtLeast(0)

        val cropLeft = (boxLeft - padding).coerceIn(0, 639)
        val cropTop = (boxTop - padding).coerceIn(0, 639)
        val cropRight = (boxRight + padding).coerceIn(0, 640)
        val cropBottom = (boxBottom + padding).coerceIn(0, 640)

        try {
            val croppedWidth = cropRight - cropLeft
            val croppedHeight = cropBottom - cropTop

            if (croppedWidth <= 0 || croppedHeight <= 0) {
                Log.e("Crop", "❌ Invalid crop dimensions.")
                return
            }

            val cropped = Bitmap.createBitmap(
                resizedBitmap,
                cropLeft,
                cropTop,
                croppedWidth,
                croppedHeight
            )

            val paddedBitmap = Bitmap.createBitmap(
                croppedWidth + padLeft + padRight,
                croppedHeight + padTop + padBottom,
                Bitmap.Config.ARGB_8888
            )

            val canvas = Canvas(paddedBitmap)
            canvas.drawColor(Color.BLACK)
            canvas.drawBitmap(cropped, padLeft.toFloat(), padTop.toFloat(), null)

            val finalScaled = Bitmap.createScaledBitmap(paddedBitmap, 1280, 1280, true)



            val mpImage = BitmapImageBuilder(finalScaled).build()
            handLandmarker.detectAsync(mpImage, SystemClock.uptimeMillis())

        } catch (e: Exception) {
            Log.e("CropScale", "❌ Error during cropping or padding", e)
        }
    }

    private fun handleLandmarkResult(result: HandLandmarkerResult) {
        if (result.landmarks().isEmpty()) return

        val landmarks = result.landmarks()[0]
        if (landmarks.size <= 8) return

        val indexTip = landmarks[8]
        val unnormalizedX = indexTip.x() * 1280f
        val unnormalizedY = indexTip.y() * 1280f

        Log.d("HandLandmarkHelper", "👆 Index finger absolute in 1280x1280 image: ($unnormalizedX, $unnormalizedY)")
        onAdjustedIndexTip(unnormalizedX, unnormalizedY)
    }

    fun close() {
        handLandmarker.close()
        tfliteHelper.close()
    }
}
