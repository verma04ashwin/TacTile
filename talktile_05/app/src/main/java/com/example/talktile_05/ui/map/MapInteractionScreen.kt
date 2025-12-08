package com.example.talktile_05.ui.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.talktile_05.viewmodel.MapInteractionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapInteractionScreen(
    book: String,
    chapter: String,
    mapJsonFile: String,
    vm: MapInteractionViewModel,
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) {
        vm.loadMap(book, chapter, mapJsonFile)
    }

    val bmp by vm.mapBitmap.collectAsState()
    val title by vm.mapTitle.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title ?: "Map", fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
        ) {
            bmp?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Map Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val w = this.size.width.toFloat()
                                val h = this.size.height.toFloat()
                                vm.onTap(offset.x, offset.y, w, h)
                            }
                        }
                        .padding(12.dp)
                )
            } ?: Text("Loading...", Modifier.padding(24.dp))

            Spacer(Modifier.height(12.dp))
        }
    }
}
