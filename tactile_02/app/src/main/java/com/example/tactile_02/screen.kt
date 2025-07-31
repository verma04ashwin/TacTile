package com.example.tactile_02

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.camera.view.PreviewView
import com.example.tactile_02.camera.CameraHandler
import com.example.tactile_02.helper.SpeechHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Screen(
    context: Context,
    selectedFile: String,                 // ✅ Passed from MainActivity
    availableJsonFiles: List<String>,
    onJsonSelected: (String) -> Unit,
    speechHelper: SpeechHelper,
    onSpeak: (String) -> Unit,
    cameraHandler: CameraHandler
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).also { previewView ->
                    cameraHandler.initCamera(previewView)
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .zIndex(1f),
            verticalArrangement = Arrangement.Top
        ) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                TextField(
                    value = selectedFile,    // ✅ Always shows the latest value
                    onValueChange = {},
                    label = { Text("Select Map JSON") },
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    availableJsonFiles.forEach { file ->
                        DropdownMenuItem(
                            text = { Text(file) },
                            onClick = {
                                onJsonSelected(file)  // ✅ Updates MainActivity variable
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { speechHelper.startListening() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🎤 Voice Select")
            }
        }
    }
}
