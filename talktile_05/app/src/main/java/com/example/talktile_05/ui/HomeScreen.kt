package com.example.talktile_05.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.talktile_05.ui.components.AccessibleButton
import com.example.talktile_05.ui.components.DropdownList
import com.example.talktile_05.ui.components.VoiceMicButton
import com.example.talktile_05.utils.findActivity
import com.example.talktile_05.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    onOpenReader: (String, String, Int) -> Unit,
    vm: HomeViewModel = viewModel()
) {
    val activity = LocalContext.current.findActivity()

    val books by vm.books.collectAsState()
    val chapters by vm.chapters.collectAsState()
    val selectedBook by vm.selectedBook.collectAsState()
    val selectedChapter by vm.selectedChapter.collectAsState()
    val pageInput by vm.pageInput.collectAsState()

    LaunchedEffect(Unit) { vm.loadBooks() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {

        Text("Talktile Reader", fontSize = 30.sp)
        Spacer(Modifier.height(24.dp))

        DropdownList(
            items = books,
            selected = selectedBook,
            onSelected = vm::selectBook,
            placeholder = "Select Book"
        )

        Spacer(Modifier.height(16.dp))

        DropdownList(
            items = chapters,
            selected = selectedChapter,
            onSelected = vm::selectChapter,
            placeholder = "Select Chapter"
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = pageInput,
            onValueChange = vm::updatePageInput,
            label = { Text("Page Number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        AccessibleButton("Open Reader") {
            val b = selectedBook ?: return@AccessibleButton
            val c = selectedChapter ?: return@AccessibleButton
            val p = pageInput.toIntOrNull() ?: 1
            onOpenReader(b, c, p)
        }

        Spacer(Modifier.height(30.dp))

        VoiceMicButton(
            contentDescription = "Start Voice Command",
            onPress = {
                activity?.let {
                    vm.startVoiceCommand(it) { book, chapter, page ->
                        onOpenReader(book, chapter, page)
                    }
                }
            }
        )
    }
}
