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
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HandLandmarkHelper(
    private val context: Context,
    modelName: String = "best_float32.tflite",
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

        // 3. Calculate bounding box in 640x640 image
        val boxLeft = ((x - w / 2f) * 640).toInt().coerceIn(0, 639)
        val boxTop = ((y - h / 2f) * 640).toInt().coerceIn(0, 639)
        val boxRight = ((x + w / 2f) * 640).toInt().coerceIn(0, 640)
        val boxBottom = ((y + h / 2f) * 640).toInt().coerceIn(0, 640)

        // 4. Expand box by 200px on all sides
        val expandedLeft = (boxLeft - 200).coerceIn(0, 639)
        val expandedTop = (boxTop - 200).coerceIn(0, 639)
        val expandedRight = (boxRight + 200).coerceIn(0, 640)
        val expandedBottom = (boxBottom + 200).coerceIn(0, 640)

        // 5. Crop expanded region and resize to 1280x1280
        try {
            val width = expandedRight - expandedLeft
            val height = expandedBottom - expandedTop

            if (width <= 0 || height <= 0) {
                Log.e("Crop", "❌ Invalid expanded crop dimensions.")
                return
            }

            val cropped = Bitmap.createBitmap(
                resizedBitmap,
                expandedLeft,
                expandedTop,
                width,
                height
            )

            val scaled = Bitmap.createScaledBitmap(cropped, 1280, 1280, true)
            saveFrame(scaled)

            // 6. Run landmark detection on the cropped+resized image
            val mpImage = BitmapImageBuilder(scaled).build()
            handLandmarker.detectAsync(mpImage, SystemClock.uptimeMillis())

        } catch (e: Exception) {
            Log.e("CropSave", "❌ Error cropping or scaling frame", e)
        }
    }

    private fun handleLandmarkResult(result: HandLandmarkerResult) {
        if (result.landmarks().isEmpty()) return

        val landmarks = result.landmarks()[0]
        if (landmarks.size <= 8) return

        val indexTip = landmarks[8]

        // Image size = 1280x1280
        val unnormalizedX = indexTip.x() * 1280f
        val unnormalizedY = indexTip.y() * 1280f

        Log.d("HandLandmarkHelper", "👆 Index finger absolute in 1280x1280 image: ($unnormalizedX, $unnormalizedY)")
        onAdjustedIndexTip(unnormalizedX, unnormalizedY)
    }

    private fun saveFrame(bitmap: Bitmap) {
        try {
            val dir = File(context.getExternalFilesDir(null), "frames")
            if (!dir.exists()) dir.mkdirs()

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
            val file = File(dir, "cropped_$timeStamp.png")

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            Log.d("FrameSaver", "✅ Cropped frame saved: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("FrameSaver", "❌ Failed to save cropped frame", e)
        }
    }

    fun close() {
        handLandmarker.close()
        tfliteHelper.close()
    }
}
