package com.example.testingtflitemodels

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
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

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val detectionText = mutableStateOf("Running model...")
        val resultBitmap = mutableStateOf<Bitmap?>(null)

        Thread {
            try {
                val inputStream: InputStream = assets.open("testingImages/frame_1751623845430.jpg")
                val originalBitmap = BitmapFactory.decodeStream(inputStream)

                val model = TFLiteHelper(this, "best_float32.tflite")
                val drawnBitmap = model.runInference(originalBitmap)
                model.close()

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
