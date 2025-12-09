package com.example.talktile_05.ml.helper

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.example.talktile_05.ml.gesture.HandGestureProcessor

/**
 * Uses MediaPipe HandLandmarker to detect hand gestures
 * (used when palmRegion is detected by YOLO).
 *
 * Calls onGestureDetected() when a gesture is recognized.
 */
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

    /**
     * Run MediaPipe hand landmark detection → classify gesture → callback
     */
    fun detectHands(bitmap: Bitmap) {
        try {
            val mpImage: MPImage = BitmapImageBuilder(bitmap).build()

            val result: HandLandmarkerResult = handLandmarker.detect(mpImage)

            if (result.landmarks().isNotEmpty()) {
                val gesture = gestureProcessor.classify(result.landmarks()[0])
                if (gesture != null) {
                    onGestureDetected(gesture)
                }
            }

        } catch (e: Exception) {
            Log.e("HandLandmarkerHelper", "Error detecting hand: ${e.message}")
        }
    }
}
