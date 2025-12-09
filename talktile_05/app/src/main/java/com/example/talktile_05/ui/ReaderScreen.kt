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
    vm: ReaderViewModel,
    onBack: () -> Unit,
    onOpenMap: (String, String, String) -> Unit     // FIXED CALLBACK SIGNATURE
) {
    val activity = LocalContext.current.findActivity()

    val paragraph by vm.currentParagraphText.collectAsState("")
    val index by vm.currentParagraphIndex.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val pageBitmap = vm.pageBitmap.value
    val currentPage by vm.currentPage.collectAsState()

    // load page on first display
    LaunchedEffect(Unit) {
        vm.open(book, chapter, page)
    }

    // listen for VM map open requests and navigate to MapInteraction screen
    LaunchedEffect(vm) {
        vm.openMapRequest.collectLatest { mapFile ->
            if (!mapFile.isNullOrEmpty()) {
                onOpenMap(book, chapter, mapFile)  // PASSES ALL 3 VALUES
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Page $currentPage") },
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

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {

                if (isLoading)
                    LinearProgressIndicator(Modifier.fillMaxWidth())

                Spacer(Modifier.height(12.dp))

                pageBitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "PDF Page",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(500.dp)
                            .padding(12.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    "Paragraph ${index + 1}",
                    fontSize = 22.sp,
                    modifier = Modifier.padding(12.dp)
                )

                Text(
                    paragraph,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AccessibleButton(text = "Prev Para", modifier = Modifier.weight(1f)) { vm.prevParagraph() }
                    AccessibleButton(text = "Next Para", modifier = Modifier.weight(1f)) { vm.nextParagraph() }
                }

                Spacer(Modifier.height(8.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AccessibleButton(text = "Prev Page", modifier = Modifier.weight(1f)) { vm.prevPage() }
                    AccessibleButton(text = "Next Page", modifier = Modifier.weight(1f)) { vm.nextPage() }
                }

                Spacer(Modifier.height(8.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AccessibleButton(text = "Pause", modifier = Modifier.weight(1f)) { vm.pauseTTS() }
                    AccessibleButton(text = "Resume", modifier = Modifier.weight(1f)) { vm.resumeTTS() }
                }

                Spacer(Modifier.height(8.dp))

                VoiceMicButton(
                    contentDescription = "Voice Command",
                    onPress = { activity?.let { vm.startVoiceCommand(it) } }
                )
            }
        }
    }
}
