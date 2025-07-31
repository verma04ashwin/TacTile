package com.example.tactile_02.ml.helper

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.tactile_02.ml.gesture.HandGestureProcessor
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.framework.image.BitmapImageBuilder

class HandLandmarkerHelper(
    context: Context,
    private val onGestureDetected: (String) -> Unit
) {

    private val handLandmarker: HandLandmarker
    private val gestureProcessor = HandGestureProcessor()

    init {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("hand_landmarker.task")
            .build()

        val options = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.IMAGE)
            .setNumHands(1)
            .build()

        handLandmarker = HandLandmarker.createFromOptions(context, options)
    }

    fun detectHands(bitmap: Bitmap) {
        try {
            // ✅ Convert Bitmap → MPImage
            val mpImage: MPImage = BitmapImageBuilder(bitmap).build()

            // ✅ Run detection
            val result: HandLandmarkerResult = handLandmarker.detect(mpImage)

            if (result.landmarks().isNotEmpty()) {
                val gesture = gestureProcessor.classify(result.landmarks()[0])
                if (gesture != null) {
                    Log.d("HandLandmarkerHelper", "Gesture detected: $gesture")
                    onGestureDetected(gesture)
                }
            }
        } catch (e: Exception) {
            Log.e("HandLandmarkerHelper", "Error in detection: ${e.message}")
        }
    }
}
