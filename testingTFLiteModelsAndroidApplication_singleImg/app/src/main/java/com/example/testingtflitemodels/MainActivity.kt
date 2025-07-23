package com.example.testingtflitemodels

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.example.testingtflitemodels.ui.theme.TestingTFLiteModelsTheme
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private fun saveToGallery(bitmap: Bitmap, displayName: String) {
        val filename = "$displayName.png"
        val resolver = contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/TFLiteDetections")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        imageUri?.let { uri ->
            resolver.openOutputStream(uri)?.use { outStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
            }
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
            Log.i("SaveToGallery", "✅ Saved cropped image to gallery: $uri")
        } ?: Log.e("SaveToGallery", "❌ Failed to create image URI")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val detectionText = mutableStateOf("Running model...")
        val resultBitmap = mutableStateOf<Bitmap?>(null)

        Thread {
            try {
                val inputStream: InputStream = assets.open("testingImages/frame_1751623918841.jpg")
                val originalBitmap = BitmapFactory.decodeStream(inputStream)

                val model = TFLiteHelper(this, "best_float32.tflite")
                val (drawnBitmap, croppedBitmap) = model.runInference(originalBitmap)
                model.close()

                // Save cropped image to gallery if found
                croppedBitmap?.let {
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    saveToGallery(it, "cropped_$timestamp")
                }

                detectionText.value = "Drew top prediction box"
                resultBitmap.value = drawnBitmap

            } catch (e: Exception) {
                Log.e("Inference", "❌ Error: ${e.message}")
                detectionText.value = "Error running model"
            }
        }.start()

        setContent {
            TestingTFLiteModelsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Top
                    ) {
                        Text(
                            text = detectionText.value,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        resultBitmap.value?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Predicted",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
