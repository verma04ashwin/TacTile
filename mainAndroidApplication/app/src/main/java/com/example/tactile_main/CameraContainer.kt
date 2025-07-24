package com.example.tactile_main

import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun CameraContainer(regionFile: String) {
    AndroidView(
        factory = { context ->
            CameraView(context).apply {
                setRegionFile(regionFile)
            }
        },
        update = { cameraView ->
            cameraView.setRegionFile(regionFile)
        }
    )
}
