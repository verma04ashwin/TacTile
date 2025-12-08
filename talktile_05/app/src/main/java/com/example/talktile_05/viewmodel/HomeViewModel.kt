package com.example.talktile_05.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.example.talktile_05.data.PdfRepository
import com.example.talktile_05.services.SttManager
import com.example.talktile_05.services.TtsManager
import com.example.talktile_05.services.VoiceCommandParser
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.talktile_05.App

class HomeViewModel(
    private val repo: PdfRepository = PdfRepository(),
    private val stt: SttManager = SttManager(),
    private val tts: TtsManager = TtsManager(App.instance),
    private val parser: VoiceCommandParser = VoiceCommandParser()
) : ViewModel() {

    val books = MutableStateFlow<List<String>>(emptyList())
    val chapters = MutableStateFlow<List<String>>(emptyList())

    val selectedBook = MutableStateFlow<String?>(null)
    val selectedChapter = MutableStateFlow<String?>(null)
    val pageInput = MutableStateFlow("1")

    fun loadBooks() {
        books.value = repo.listBooks().sorted()
    }

    fun selectBook(book: String) {
        selectedBook.value = book
        chapters.value = repo.listChapters(book).sorted()
    }

    fun selectChapter(ch: String) {
        selectedChapter.value = ch
    }

    fun updatePageInput(text: String) {
        pageInput.value = text.filter { it.isDigit() }
    }

    fun startVoiceCommand(activity: Activity, onOpenReader: (String, String, Int) -> Unit) {
        stt.startListening(activity) { spoken ->
            handleVoice(spoken, onOpenReader)
        }
    }

    private fun handleVoice(raw: String, onOpenReader: (String, String, Int) -> Unit) {
        val cmd = parser.parse(raw)

        // ---------------- Book ----------------
        if (cmd.book != null) {
            val match = parser.bestMatch(cmd.book, books.value)
            if (match == null) {
                tts.speak("Book not found.")
                return
            }
            selectBook(match)
        } else if (selectedBook.value == null) {
            tts.speak("Please say the book name.")
            return
        }

        // ---------------- Chapter ----------------
        if (cmd.chapter != null) {
            val match = parser.bestMatch(cmd.chapter!!, chapters.value)
            if (match == null) {
                tts.speak("Chapter not found.")
                return
            }
            selectChapter(match)
        } else if (selectedChapter.value == null) {
            tts.speak("Please say the chapter name.")
            return
        }

        // ---------------- Page ----------------
        val page = cmd.page
        if (page == null) {
            tts.speak("Tell me the page number.")
            return
        }

        val book = selectedBook.value!!
        val chapter = selectedChapter.value!!

        tts.speak("Opening $book, chapter $chapter, page $page.")
        onOpenReader(book, chapter, page)
    }
}
