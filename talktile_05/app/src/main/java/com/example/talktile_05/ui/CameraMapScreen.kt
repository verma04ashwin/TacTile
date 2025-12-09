package com.example.talktile_05.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.talktile_05.camera.CameraHandler
import com.example.talktile_05.data.RegionManager
import com.example.talktile_05.ml.TFLiteHelper
import com.example.talktile_05.ml.helper.HandLandmarkerHelper
import com.example.talktile_05.services.TtsManager
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraMapScreen(
    book: String,
    chapter: String,
    mapJsonFile: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // 1 — Load polygons
    val regionManager = remember { RegionManager(context) }
    LaunchedEffect(mapJsonFile) {
        Log.d("CameraMapScreen", "About to load map JSON: $book/$chapter/$mapJsonFile")
        regionManager.loadRegionsFromAsset("$book/$chapter/$mapJsonFile")
    }


    // 2 — TTS
    val ttsManager = remember { TtsManager(context) }

    // 3 — Hand gesture detection
    val handHelper = remember {
        HandLandmarkerHelper(context) { gesture ->
            ttsManager.speak(gesture)
        }
    }

    // 4 — TFLite YOLO → Region mapping
    val tfliteHelper = remember {
        TFLiteHelper(
            context = context,
            tts = ttsManager,
            handLandmarkerHelper = handHelper,
            regionManager = regionManager
        )
    }

    // 5 — Camera
    val cameraHandler = remember { CameraHandler(context, tfliteHelper) }

    // 6 — Permission
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        if (!granted) ttsManager.speak("Camera permission is required to explore the map.")
    }

    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            delay(100)
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    // UI Layout
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(mapJsonFile) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->

        if (permissionGranted) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        post { cameraHandler.initCamera(this) }
                    }
                }
            )
        } else {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Text("Camera permission required.")
            }
        }
    }
}
