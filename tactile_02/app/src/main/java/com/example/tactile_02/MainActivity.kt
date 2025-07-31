package com.example.tactile_02

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.example.tactile_02.data.RegionManager
import com.example.tactile_02.ml.TFLiteHelper
import com.example.tactile_02.ml.helper.HandLandmarkerHelper
import com.example.tactile_02.speech.TTSHelper
import com.example.tactile_02.camera.CameraHandler
import com.example.tactile_02.helper.SpeechHelper

class MainActivity : ComponentActivity() {

    private val permissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    private lateinit var speechHelper: SpeechHelper
    private lateinit var regionManager: RegionManager
    private lateinit var ttsHelper: TTSHelper
    private lateinit var tfliteHelper: TFLiteHelper
    private lateinit var cameraHandler: CameraHandler
    private lateinit var handLandmarkerHelper: HandLandmarkerHelper

    private var currentJsonFile by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        regionManager = RegionManager(this)
        ttsHelper = TTSHelper(this)

        // Default file if no saved state
        currentJsonFile = regionManager.getCurrentFileName()
            ?: "India's Climate Pattern.json"
        regionManager.loadRegionsFromAsset(currentJsonFile)

        val speechLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                speechHelper.handleResult(result.data)
            }
        }

        // ✅ Flexible voice command mapping + fallback
        speechHelper = SpeechHelper(speechLauncher) { spokenText ->
            val normalized = spokenText.lowercase()

            val file = when {
                "climate" in normalized -> "India's Climate Pattern.json"
                "soil" in normalized -> "India's Soil Pattern.json"
                "cities" in normalized -> "Indian Cities.json"
                else -> null
            }

            if (file != null) {
                updateCurrentJson(file)
                val spokenName = file.removeSuffix(".json")
                ttsHelper.speak("Map changed to $spokenName")
            } else {
                ttsHelper.speak("Sorry, I didn't understand that.")
            }
        }

        handLandmarkerHelper = HandLandmarkerHelper(
            context = this,
            onGestureDetected = { gesture ->
                if (gesture == "closed_fist") {
                    runOnUiThread { speechHelper.startListening() }
                }
            }
        )

        tfliteHelper = TFLiteHelper(
            context = this,
            ttsHelper = ttsHelper,
            handLandmarkerHelper = handLandmarkerHelper,
            regionManager = regionManager
        )

        cameraHandler = CameraHandler(this, tfliteHelper)

        if (allPermissionsGranted()) {
            setupUI()
        } else {
            requestPermissionsLauncher.launch(permissions)
        }
    }

    private fun updateCurrentJson(file: String) {
        try {
            regionManager.loadRegionsFromAsset(file) // load and set internally
            currentJsonFile = file                   // update Compose state
            tfliteHelper.onRegionDataUpdated()       // ✅ notify downstream logic
            val spokenName = file.removeSuffix(".json")
            ttsHelper.speak("Map changed to $spokenName")
        } catch (e: Exception) {
            ttsHelper.speak("Failed to load map.")
            Log.e("MainActivity", "Error loading $file", e)
        }
    }


    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissionsResult ->
            if (permissionsResult.values.all { it }) {
                setupUI()
            }
        }

    private fun setupUI() {
        val availableJsonFiles = listOf(
            "India's Climate Pattern.json",
            "India's Soil Pattern.json",
            "Indian Cities.json"
        )

        setContent {
            Screen(
                context = this,
                selectedFile = currentJsonFile,
                availableJsonFiles = availableJsonFiles,
                onJsonSelected = { file ->
                    updateCurrentJson(file)
                    val spokenName = file.removeSuffix(".json")
                    ttsHelper.speak("Map changed to $spokenName")
                },
                speechHelper = speechHelper,
                onSpeak = { text -> ttsHelper.speak(text) },
                cameraHandler = cameraHandler
            )
        }
    }

    private fun allPermissionsGranted(): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onDestroy() {
        ttsHelper.shutdown()
        super.onDestroy()
    }
}
