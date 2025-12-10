package com.example.talktile_05.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collectLatest
import com.example.talktile_05.ui.components.AccessibleButton
import com.example.talktile_05.ui.components.VoiceMicButton
import com.example.talktile_05.utils.findActivity
import com.example.talktile_05.viewmodel.ReaderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    book: String,
    chapter: String,
    page: Int,
    paragraph: Int,
    line: Int,
    vm: ReaderViewModel,
    onBack: () -> Unit,
    onOpenMap: (String, String, String) -> Unit
) {
    val activity = LocalContext.current.findActivity()

    val paragraphText by vm.currentParagraphText.collectAsState("")
    val index by vm.currentParagraphIndex.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val bitmap = vm.pageBitmap.value
    val currentPage by vm.currentPage.collectAsState()
    val mapFile by vm.mapForCurrentPage.collectAsState()

    LaunchedEffect(Unit) {
        vm.open(book, chapter, page, paragraph, line)
    }

    LaunchedEffect(Unit) {
        vm.openMapRequest.collectLatest { mf ->
            onOpenMap(book, chapter, mf)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Page $currentPage") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                if (isLoading)
                    LinearProgressIndicator(Modifier.fillMaxWidth())

                Spacer(Modifier.height(12.dp))

                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Page content",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(500.dp)
                            .padding(12.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text("Paragraph ${index + 1}", fontSize = 22.sp, modifier = Modifier.padding(12.dp))
                Text(paragraphText, fontSize = 16.sp, modifier = Modifier.padding(16.dp))
            }

            Column(Modifier.padding(12.dp)) {

                Button(
                    onClick = {
                        if (mapFile != null) onOpenMap(book, chapter, mapFile!!)
                        else vm.speak("There is no map on this page.")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(65.dp)
                ) {
                    Text("Open Map", fontSize = 20.sp)
                }

                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    AccessibleButton("Prev Para", Modifier.weight(1f)) { vm.prevParagraph() }
                    AccessibleButton("Next Para", Modifier.weight(1f)) { vm.nextParagraph() }
                }

                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    AccessibleButton("Prev Page", Modifier.weight(1f)) { vm.prevPage() }
                    AccessibleButton("Next Page", Modifier.weight(1f)) { vm.nextPage() }
                }

                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    AccessibleButton("Pause", Modifier.weight(1f)) { vm.pauseTTS() }
                    AccessibleButton("Resume", Modifier.weight(1f)) { vm.resumeTTS() }
                }

                Spacer(Modifier.height(16.dp))

                VoiceMicButton(
                    contentDescription = "Voice Command",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(75.dp)
                ) {
                    activity?.let { vm.startVoiceCommand(it) }
                }
            }
        }
    }
}
