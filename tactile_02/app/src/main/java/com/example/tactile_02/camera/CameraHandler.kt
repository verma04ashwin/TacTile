package com.example.tactile_02.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.tactile_02.ml.TFLiteHelper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraHandler(private val context: Context, private val tfliteHelper: TFLiteHelper) {

    private lateinit var cameraExecutor: ExecutorService

    fun initCamera(previewView: PreviewView) {
        cameraExecutor = Executors.newSingleThreadExecutor()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        try {
                            tfliteHelper.runInference(imageProxy)
                        } catch (e: Exception) {
                            Log.e("CameraHandler", "Error in inference: ${e.message}")
                            imageProxy.close()
                        }
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    context as androidx.lifecycle.LifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalyzer
                )
            } catch (exc: Exception) {
                Log.e("CameraHandler", "Camera init failed: ${exc.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }
}
