package com.example.tactile_main

import android.content.Context
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
import java.util.concurrent.Executors
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

class CameraView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private lateinit var previewView: PreviewView
    private lateinit var handLandmarkHelper: HandLandmarkHelper

    init {
        previewView = PreviewView(context)
        addView(previewView)
        initHandLandmarkHelper()
        setupCamera()
    }

    private fun initHandLandmarkHelper() {
        handLandmarkHelper = HandLandmarkHelper(context) { result, input ->
            onResults(result, input)
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

            imageAnalyzer.setAnalyzer(cameraExecutor) { imageProxy ->
                val bitmap = imageProxy.toBitmap()
                handLandmarkHelper.detect(bitmap)
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

    private var regions: List<Region>? = null

    private fun loadRegionsFromAssets(): List<Region> {
        val assetManager = context.assets
        val inputStream = assetManager.open("flat_segmentation_only.json")
        val json = inputStream.bufferedReader().use { it.readText() }
        val type = object : com.google.gson.reflect.TypeToken<List<Region>>() {}.type
        return com.google.gson.Gson().fromJson(json, type)
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
            val tip = landmarks[8] // index finger tip

            // Assume 480x640 image if you're using consistent capture resolution
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




}
