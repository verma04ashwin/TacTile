package com.example.tactile_02.ml

import android.content.Context
import android.graphics.*
import android.util.Log
import androidx.camera.core.ImageProxy
import com.example.tactile_02.ml.helper.YuvToRgbConverter
import com.example.tactile_02.speech.TTSHelper
import com.example.tactile_02.ml.helper.HandLandmarkerHelper
import com.example.tactile_02.data.RegionManager
import com.example.tactile_02.utils.BoundingBoxProcessor
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil

class TFLiteHelper(
    private val context: Context,
    private val ttsHelper: TTSHelper,
    private val handLandmarkerHelper: HandLandmarkerHelper,
    private val regionManager: RegionManager
) {
    private val TAG = "TFLiteHelper"
    private val inputSize = 640
    private val labels = arrayOf("fingerRegion", "mapRegion", "palmRegion")
    private val numClasses = labels.size
    private val classThreshold = 0.01f
    private val actionThreshold_finger = 0.7f
    private val actionThreshold_palm = 0.65f

    private val interpreter: Interpreter by lazy {
        val model = FileUtil.loadMappedFile(context, "yolo_float16.tflite")
        val options = Interpreter.Options().apply { setNumThreads(4) }
        Log.d(TAG, "TFLite model loaded with ${options.numThreads} threads")
        Interpreter(model, options)
    }

    private val yuvConverter = YuvToRgbConverter()
    private var lastSpokenRegion: String? = null

    fun runInference(imageProxy: ImageProxy) {
        try {
            val originalBitmap = imageProxyToBitmap(imageProxy)
            val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, inputSize, inputSize, true)

            val inputTensor = preprocess(resizedBitmap)
            val outputArray = Array(1) { Array(7) { FloatArray(8400) } }

            interpreter.run(inputTensor, outputArray)

            logAndCropMapRegion(outputArray[0], originalBitmap)
            triggerActions(outputArray[0], originalBitmap)

        } catch (e: Exception) {
            Log.e(TAG, "Error in runInference: ${e.message}")
        } finally {
            imageProxy.close()
        }
    }

    private fun preprocess(bitmap: Bitmap): Array<Array<Array<FloatArray>>> {
        val input = Array(1) { Array(inputSize) { Array(inputSize) { FloatArray(3) } } }
        for (y in 0 until inputSize) {
            for (x in 0 until inputSize) {
                val pixel = bitmap.getPixel(x, y)
                input[0][y][x][0] = Color.red(pixel) / 255f
                input[0][y][x][1] = Color.green(pixel) / 255f
                input[0][y][x][2] = Color.blue(pixel) / 255f
            }
        }
        return input
    }

    private fun logAndCropMapRegion(detections: Array<FloatArray>, originalBitmap: Bitmap) {
        val width = originalBitmap.width
        val height = originalBitmap.height

        val bestScores = FloatArray(numClasses) { -1f }
        val bestBoxes = Array(numClasses) { FloatArray(4) }

        for (i in detections[0].indices) {
            val x = detections[0][i]
            val y = detections[1][i]
            val w = detections[2][i]
            val h = detections[3][i]

            for (cls in 0 until numClasses) {
                val score = detections[4 + cls][i]
                if (score > bestScores[cls]) {
                    bestScores[cls] = score
                    bestBoxes[cls] = floatArrayOf(x, y, w, h)
                }
            }
        }

        for (cls in 0 until numClasses) {
            Log.d(TAG, "Best Box for ${labels[cls]} → Score: ${bestScores[cls]}, Box: ${bestBoxes[cls].contentToString()}")
        }

        val mapIdx = 1
        if (bestScores[mapIdx] > classThreshold) {
            val rect = toRectF(bestBoxes[mapIdx], width, height)

            val cropLeft = rect.left.toInt().coerceAtLeast(0)
            val cropTop = rect.top.toInt().coerceAtLeast(0)
            val cropWidth = (rect.width()).toInt().coerceAtMost(width - cropLeft)
            val cropHeight = (rect.height()).toInt().coerceAtMost(height - cropTop)

            if (cropWidth > 0 && cropHeight > 0) {
                // Cropped bitmap is created but NOT saved anymore
                Bitmap.createBitmap(originalBitmap, cropLeft, cropTop, cropWidth, cropHeight)
                Log.d(TAG, "Cropped mapRegion computed: left=$cropLeft top=$cropTop w=$cropWidth h=$cropHeight")
            } else {
                Log.e(TAG, "Invalid crop size: width=$cropWidth, height=$cropHeight")
            }
        }
    }

    private fun triggerActions(detections: Array<FloatArray>, originalBitmap: Bitmap) {
        val bestScores = FloatArray(numClasses) { -1f }
        val bestBoxes = Array(numClasses) { FloatArray(4) }

        for (i in detections[0].indices) {
            val x = detections[0][i]
            val y = detections[1][i]
            val w = detections[2][i]
            val h = detections[3][i]

            for (cls in 0 until numClasses) {
                val score = detections[4 + cls][i]
                if (score > bestScores[cls]) {
                    bestScores[cls] = score
                    bestBoxes[cls] = floatArrayOf(x, y, w, h)
                }
            }
        }

        val width = originalBitmap.width
        val height = originalBitmap.height

        if (bestScores[0] > actionThreshold_finger && bestScores[1] > actionThreshold_finger) {
            val fingerBox = toRectF(bestBoxes[0], width, height)
            val mapBox = toRectF(bestBoxes[1], width, height)

            val (_, fingerCenter) = BoundingBoxProcessor.processAndAdjustFingerCenter(
                originalBitmap, mapBox, fingerBox
            )

            val (cx, cy) = fingerCenter
            Log.d("RegionCheck", "Calling getTouchedRegion with finger center=($cx, $cy), regions size=${regionManager.getRegions().size}")

            val region = regionManager.getTouchedRegion(cx, cy)
            Log.d("RegionCheck", "Result from getTouchedRegion: $region")

            if (region != null && region != lastSpokenRegion) {
                lastSpokenRegion = region
                ttsHelper.speak(region)
                Log.d(TAG, "Finger detected region: $region")
            }
        }

        if (bestScores[2] > actionThreshold_palm) {
            Log.d(TAG, "Palm detected (score: ${bestScores[2]}) → handLandmarker")
            handLandmarkerHelper.detectHands(originalBitmap)
        }
    }

    private fun toRectF(box: FloatArray, width: Int, height: Int): RectF {
        val (x, y, w, h) = box
        val left = (x - w / 2) * width
        val top = (y - h / 2) * height
        val right = (x + w / 2) * width
        val bottom = (y + h / 2) * height
        return RectF(left, top, right, bottom)
    }

    fun onRegionDataUpdated() {
        // Optional: log update or clear cached state if any
        Log.d("TFLiteHelper", "Region data updated from RegionManager")
        // Add logic if you maintain internal region state that needs refreshing
    }
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        return yuvConverter.yuvToRgb(imageProxy)
    }
}
