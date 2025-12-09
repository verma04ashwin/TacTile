package com.example.talktile_05.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.talktile_05.ml.TFLiteHelper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * CameraHandler
 *
 * - Starts CameraX
 * - Streams camera frames to TFLiteHelper
 * - Provides PreviewView for UI
 */
class CameraHandler(
    private val context: Context,
    private val tfliteHelper: TFLiteHelper
) {

    private lateinit var cameraExecutor: ExecutorService

    fun initCamera(previewView: PreviewView) {
        cameraExecutor = Executors.newSingleThreadExecutor()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                // Preview surface
                val preview = Preview.Builder()
                    .build()
                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                // Analyzer for ML inference
                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor) { imageProxy ->
                            try {
                                tfliteHelper.runInference(imageProxy)
                            } catch (e: Exception) {
                                Log.e("CameraHandler", "Inference error: ${e.message}")
                                imageProxy.close()
                            }
                        }
                    }

                // Bind camera
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    context as LifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalyzer
                )

            } catch (e: Exception) {
                Log.e("CameraHandler", "Camera initialization failed: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }
}
