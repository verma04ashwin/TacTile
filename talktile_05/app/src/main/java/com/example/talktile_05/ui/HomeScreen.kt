package com.example.talktile_05.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
    onOpenReader: (String, String, Int, Int, Int) -> Unit,
    vm: HomeViewModel = viewModel()
) {
    val activity = LocalContext.current.findActivity()

    val books by vm.books.collectAsState()
    val chapters by vm.chapters.collectAsState()
    val selectedBook by vm.selectedBook.collectAsState()
    val selectedChapter by vm.selectedChapter.collectAsState()
    val pageInput by vm.pageInput.collectAsState()
    val paragraphInput by vm.paragraphInput.collectAsState()
    val lineInput by vm.lineInput.collectAsState()

    LaunchedEffect(Unit) { vm.loadBooks() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp)
            .verticalScroll(rememberScrollState())
    ) {

        Text(
            text = "Talktile Reader",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics {
                contentDescription = "Talktile Reader Home Screen"
            }
        )

        Spacer(Modifier.height(32.dp))

        DropdownList(
            items = books,
            selected = selectedBook,
            onSelected = vm::selectBook,
            placeholder = "Select Book"
        )

        Spacer(Modifier.height(20.dp))

        DropdownList(
            items = chapters,
            selected = selectedChapter,
            onSelected = vm::selectChapter,
            placeholder = "Select Chapter"
        )

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = pageInput,
            onValueChange = vm::updatePageInput,
            label = { Text("Page Number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(fontSize = 20.sp)
        )

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = paragraphInput,
            onValueChange = vm::updateParagraphInput,
            label = { Text("Paragraph (optional)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(fontSize = 20.sp)
        )

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = lineInput,
            onValueChange = vm::updateLineInput,
            label = { Text("Line (optional)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(fontSize = 20.sp)
        )

        Spacer(Modifier.height(30.dp))

        AccessibleButton(
            text = "Start Reading",
            modifier = Modifier.fillMaxWidth()
        ) {
            val b = selectedBook ?: return@AccessibleButton
            val c = selectedChapter ?: return@AccessibleButton

            val p = pageInput.toIntOrNull() ?: 1
            val para = paragraphInput.toIntOrNull() ?: 1
            val line = lineInput.toIntOrNull() ?: 1

            onOpenReader(b, c, p, para, line)
        }

        Spacer(Modifier.height(40.dp))

        VoiceMicButton(
            contentDescription = "Voice Command",
            modifier = Modifier.size(80.dp)
        ) {
            activity?.let {
                vm.startVoiceCommand(it) { book, chapter, page, para, line ->
                    onOpenReader(book, chapter, page, para, line)
                }
            }
        }
    }
}
