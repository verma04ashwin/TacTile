package com.example.tactile_main

import android.content.Context
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.concurrent.Executors

class CameraView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs), TextToSpeech.OnInitListener {

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private lateinit var previewView: PreviewView
    private lateinit var handLandmarkHelper: HandLandmarkHelper
    private var regionFileName = "indianCities.json"
    private var regions: List<Region>? = null
    private var tts: TextToSpeech = TextToSpeech(context, this)
    private var lastSpokenRegion: String? = null

    init {
        previewView = PreviewView(context)
        addView(previewView)
        initHandLandmarkHelper()
        setupCamera()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = java.util.Locale.US
        } else {
            Log.e("TTS", "Initialization failed")
        }
    }

    fun setRegionFile(fileName: String) {
        if (regionFileName != fileName) {
            regionFileName = fileName
            regions = loadRegionsFromAssets() // ⬅️ immediately reload
            Log.d("CameraView", "Region file updated: $fileName")
        }
    }

    private fun onIndexFingerDetected(x: Float, y: Float) {
        if (regions == null) {
            regions = loadRegionsFromAssets()
        }

        var matchedRegion: String? = null

        regions?.forEach { region ->
            region.segmentation.forEach { polygon ->
                if (isPointInPolygon(x, y, polygon)) {
                    matchedRegion = region.category
                    Log.d("RegionMatch", "✅ Index finger is in region: ${region.category}")
                }
            }
        }

        matchedRegion?.let {
            if (it != lastSpokenRegion) {
                tts.speak(it, TextToSpeech.QUEUE_FLUSH, null, null)
            }
            lastSpokenRegion = it
        } ?: run {
            Log.d("RegionMatch", "❌ Index finger is NOT inside any region.")
            lastSpokenRegion = null
        }

        Log.d("IndexTip", "🧠 Detected index fingertip at x=$x, y=$y")
    }

    private fun initHandLandmarkHelper() {
        handLandmarkHelper = HandLandmarkHelper(context) { x, y ->
            onIndexFingerDetected(x, y)
        }
    }

    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            var lastAnalyzedTime = 0L

            imageAnalyzer.setAnalyzer(cameraExecutor) { imageProxy ->
                val currentTime = SystemClock.elapsedRealtime()

                // Limit processing to 1 frame every 200ms (~5 FPS)
                if (currentTime - lastAnalyzedTime >= 1000) {
                    val bitmap = imageProxy.toBitmap()
                    handLandmarkHelper.detect(bitmap)
                    lastAnalyzedTime = currentTime
                }
                imageProxy.close()
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.bindToLifecycle(
                context as LifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer
            )
        }, ContextCompat.getMainExecutor(context))
    }

    private fun loadRegionsFromAssets(): List<Region> {
        return try {
            val inputStream = context.assets.open(regionFileName)
            val json = inputStream.bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<Region>>() {}.type
            Gson().fromJson(json, type)
        } catch (e: Exception) {
            Log.e("RegionLoader", "Failed to load $regionFileName", e)
            emptyList()
        }
    }

    private fun isPointInPolygon(x: Float, y: Float, polygon: List<Float>): Boolean {
        var inside = false
        val n = polygon.size / 2
        var j = n - 1
        for (i in 0 until n) {
            val xi = polygon[2 * i]
            val yi = polygon[2 * i + 1]
            val xj = polygon[2 * j]
            val yj = polygon[2 * j + 1]

            val intersect = ((yi > y) != (yj > y)) &&
                    (x < (xj - xi) * (y - yi) / ((yj - yi) + 0.00001f) + xi)
            if (intersect) inside = !inside
            j = i
        }
        return inside
    }

    private fun onResults(result: HandLandmarkerResult, input: MPImage) {
        if (regions == null) {
            regions = loadRegionsFromAssets()
        }

        val allHands = result.landmarks()
        if (allHands.isNotEmpty()) {
            val landmarks = allHands[0]
            val tip = landmarks[8] // Index fingertip

            val imgWidth = 480f
            val imgHeight = 640f
            val x = tip.x() * imgWidth
            val y = tip.y() * imgHeight

            regions?.forEach { region ->
                region.segmentation.forEach { polygon ->
                    if (isPointInPolygon(x, y, polygon)) {
                        Log.d("RegionMatch", "Index finger is in region: ${region.category}")
                    }
                }
            }

            Log.d("IndexTip", "x=$x, y=$y")
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        tts.stop()
        tts.shutdown()
    }
}
