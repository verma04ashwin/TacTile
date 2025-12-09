package com.example.talktile_05.ml

import android.content.Context
import android.graphics.*
import android.util.Log
import androidx.camera.core.ImageProxy
import com.example.talktile_05.data.RegionManager
import com.example.talktile_05.ml.helper.HandLandmarkerHelper
import com.example.talktile_05.ml.helper.YuvToRgbConverter
import com.example.talktile_05.services.TtsManager
import com.example.talktile_05.utils.BoundingBoxProcessor
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import android.graphics.RectF

class TFLiteHelper(
    private val context: Context,
    private val tts: TtsManager,
    private val handLandmarkerHelper: HandLandmarkerHelper,
    private val regionManager: RegionManager
) {

    private val TAG = "TFLiteHelper"

    private val inputSize = 640
    private val labels = arrayOf("fingerRegion", "mapRegion", "palmRegion")
    private val numClasses = labels.size

    private val fingerThreshold = 0.70f
    private val mapThreshold = 0.70f
    private val palmThreshold = 0.90f   // STRONGER threshold

    // Palm cooldown so it doesn’t spam
    private var lastGestureTime = 0L
    private val gestureCooldown = 1200L

    // ------------------------------------------------------------
    // Load YOLO Model
    // ------------------------------------------------------------
    private fun loadModelFile(context: Context, modelName: String): ByteBuffer {
        val fd = context.assets.openFd(modelName)
        val input = FileInputStream(fd.fileDescriptor)
        val channel = input.channel
        return channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
    }

    private val interpreter: Interpreter by lazy {
        val modelBuffer = loadModelFile(context, "yolo_float16.tflite")
        val options = Interpreter.Options().apply { setNumThreads(4) }
        Interpreter(modelBuffer, options).also {
            Log.d(TAG, "YOLO model loaded.")
        }
    }

    private val yuvConverter = YuvToRgbConverter()
    private var lastSpokenRegion: String? = null

    // ------------------------------------------------------------
    // MAIN LOOP
    // ------------------------------------------------------------
    fun runInference(imageProxy: ImageProxy) {
        try {
            val original = imageProxyToBitmap(imageProxy)
            val resized = Bitmap.createScaledBitmap(original, inputSize, inputSize, true)

            val inputTensor = preprocess(resized)
            val outputTensor = Array(1) { Array(7) { FloatArray(8400) } }

            interpreter.run(inputTensor, outputTensor)

            handleDetections(outputTensor[0], original)

        } catch (e: Exception) {
            Log.e(TAG, "Inference error: ${e.message}")
        } finally {
            imageProxy.close()
        }
    }

    // ------------------------------------------------------------
    // Preprocess
    // ------------------------------------------------------------
    private fun preprocess(bitmap: Bitmap): Array<Array<Array<FloatArray>>> {
        val input = Array(1) { Array(inputSize) { Array(inputSize) { FloatArray(3) } } }
        for (y in 0 until inputSize) {
            for (x in 0 until inputSize) {
                val p = bitmap.getPixel(x, y)
                input[0][y][x][0] = Color.red(p) / 255f
                input[0][y][x][1] = Color.green(p) / 255f
                input[0][y][x][2] = Color.blue(p) / 255f
            }
        }
        return input
    }

    // ------------------------------------------------------------
    // Detection -> Finger + Map -> Region Mapping
    // ------------------------------------------------------------
    private fun handleDetections(dets: Array<FloatArray>, original: Bitmap) {
        val w = original.width
        val h = original.height

        val bestScores = FloatArray(numClasses) { -1f }
        val bestBoxes = Array(numClasses) { FloatArray(4) }

        // Pick best-scoring box per class
        for (i in dets[0].indices) {
            val x = dets[0][i]
            val y = dets[1][i]
            val bw = dets[2][i]
            val bh = dets[3][i]

            for (cls in 0 until numClasses) {
                val score = dets[4 + cls][i]
                if (score > bestScores[cls]) {
                    bestScores[cls] = score
                    bestBoxes[cls] = floatArrayOf(x, y, bw, bh)
                }
            }
        }

        val fingerScore = bestScores[0]
        val mapScore = bestScores[1]
        val palmScore = bestScores[2]

        // ------------------------------------------------------------
        // FINGER + MAP DETECTED → REGION LOGIC
        // ------------------------------------------------------------
        if (fingerScore > fingerThreshold && mapScore > mapThreshold) {
            val fingerBox = toRectF(bestBoxes[0], w, h)
            val mapBox = toRectF(bestBoxes[1], w, h)

            // Convert finger to canonical coordinate system
            val (_, center) =
                BoundingBoxProcessor.processAndAdjustFingerCenter(original, mapBox, fingerBox)

            val region = regionManager.getTouchedRegion(center.first, center.second)

            if (region != null && region != lastSpokenRegion) {
                lastSpokenRegion = region
                tts.speak(region)
            }

            // IMPORTANT: Return early → ignore palm when exploring map
            return
        }

        // ------------------------------------------------------------
        // PALM MODE (ONLY when finger + map NOT detected)
        // ------------------------------------------------------------
        if (fingerScore < fingerThreshold &&
            mapScore < mapThreshold &&
            palmScore > palmThreshold) {

            val now = System.currentTimeMillis()
            if (now - lastGestureTime > gestureCooldown) {
                handLandmarkerHelper.detectHands(original)
                lastGestureTime = now
            }
        }
    }

    private fun toRectF(box: FloatArray, w: Int, h: Int): RectF {
        val (x, y, bw, bh) = box
        return RectF(
            (x - bw / 2f) * w,
            (y - bh / 2f) * h,
            (x + bw / 2f) * w,
            (y + bh / 2f) * h
        )
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        return yuvConverter.yuvToRgb(imageProxy)
    }

    fun onRegionDataUpdated() {
        lastSpokenRegion = null
    }
}
