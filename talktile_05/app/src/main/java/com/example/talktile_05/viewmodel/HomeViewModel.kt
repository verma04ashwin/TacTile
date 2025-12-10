package com.example.talktile_05.viewmodel

import android.app.Activity
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import com.example.talktile_05.data.PdfRepository
import com.example.talktile_05.services.FuzzyMatcher
import com.example.talktile_05.services.SttManager
import com.example.talktile_05.services.TtsManager
import com.example.talktile_05.services.VoiceCommandParser
import com.example.talktile_05.services.ParsedCommand
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
    val paragraphInput = MutableStateFlow("")
    val lineInput = MutableStateFlow("")

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

    fun updateParagraphInput(text: String) {
        paragraphInput.value = text.filter { it.isDigit() }
    }

    fun updateLineInput(text: String) {
        lineInput.value = text.filter { it.isDigit() }
    }

    /**
     * Start voice listening, but wait while TTS is speaking to avoid mic interference.
     */
    fun startVoiceCommand(
        activity: Activity,
        onOpenReader: (String, String, Int, Int, Int) -> Unit
    ) {
        // if TTS is speaking, poll until it's free (non-blocking)
        if (tts.isSpeaking()) {
            Handler(Looper.getMainLooper()).postDelayed({
                startVoiceCommand(activity, onOpenReader)
            }, 350)
            return
        }

        stt.startListening(activity) { spoken ->
            handleVoice(spoken, onOpenReader)
        }
    }

    private fun handleVoice(
        raw: String,
        onOpenReader: (String, String, Int, Int, Int) -> Unit
    ) {
        val cmd = parser.parse(raw)

        // QUICK: if unknown, prompt and return
        if (cmd.action == ParsedCommand.Action.UNKNOWN) {
            tts.speak("Sorry, I did not understand. Please say page, chapter or book.")
            return
        }

        // If fast actions
        when (cmd.action) {
            ParsedCommand.Action.OPEN_MAP -> {
                // We need book & chapter to open reader's map; attempt to use selected if exists
                if (selectedBook.value == null || selectedChapter.value == null) {
                    tts.speak("Please open a book and chapter first or say them.")
                    return
                }
                // We will open the reader UI from the caller; here we'll call onOpenReader with current page
                val p = pageInput.value.toIntOrNull() ?: 1
                val para = paragraphInput.value.toIntOrNull() ?: 1
                val ln = lineInput.value.toIntOrNull() ?: 1
                onOpenReader(selectedBook.value!!, selectedChapter.value!!, p, para, ln)
                return
            }
            ParsedCommand.Action.NEXT_PAGE -> {
                // delegate to UI: easiest is to open current with +1 page
                val p = (pageInput.value.toIntOrNull() ?: 1) + 1
                val para = 1
                val ln = 1
                if (selectedBook.value == null || selectedChapter.value == null) {
                    tts.speak("Please say the book and chapter to navigate pages.")
                    return
                }
                tts.speak("Opening page $p")
                onOpenReader(selectedBook.value!!, selectedChapter.value!!, p, para, ln)
                return
            }
            ParsedCommand.Action.PREV_PAGE -> {
                val current = pageInput.value.toIntOrNull() ?: 1
                val p = if (current > 1) current - 1 else 1
                val para = 1
                val ln = 1
                if (selectedBook.value == null || selectedChapter.value == null) {
                    tts.speak("Please say the book and chapter to navigate pages.")
                    return
                }
                tts.speak("Opening page $p")
                onOpenReader(selectedBook.value!!, selectedChapter.value!!, p, para, ln)
                return
            }
            else -> {
                // fall through to below handling
            }
        }

        // Try to resolve book and chapter using the parser candidates (parser returns loose phrases)
        var resolvedBook = selectedBook.value
        var resolvedChapter = selectedChapter.value

        // If parser provides book/chapter text, fuzzy-match them
        if (!cmd.book.isNullOrBlank()) {
            val cand = cmd.book.trim()
            FuzzyMatcher.bestMatch(cand, books.value)?.let { matched ->
                resolvedBook = matched
                selectBook(matched)
            }
        }
        if (!cmd.chapter.isNullOrBlank()) {
            val cand = cmd.chapter.trim()
            // if chapters list is empty (no book selected yet), try to load chapters from resolvedBook
            if (resolvedBook != null && chapters.value.isEmpty()) {
                chapters.value = repo.listChapters(resolvedBook!!).sorted()
            }
            FuzzyMatcher.bestMatch(cand, chapters.value)?.let { matched ->
                resolvedChapter = matched
                selectChapter(matched)
            }
        }

        // If still missing, attempt a fuzzy match of any leftover candidate phrase against books then chapters
        if ((resolvedBook == null || resolvedChapter == null) && !cmd.book.isNullOrBlank()) {
            val cand = cmd.book.trim()
            if (resolvedBook == null) {
                FuzzyMatcher.bestMatch(cand, books.value)?.let {
                    resolvedBook = it
                    selectBook(it)
                }
            }
            if (resolvedChapter == null) {
                // maybe candidate contains both; try splitting on common separators
                val parts = cand.split(Regex(" and | - | & | :,|,")).map { it.trim() }.filter { it.isNotBlank() }
                parts.forEach { p ->
                    FuzzyMatcher.bestMatch(p, chapters.value)?.let { matched ->
                        resolvedChapter = matched
                        selectChapter(matched)
                    }
                }
            }
        }

        // If we still don't have a book/chapter, but parser returned only page/para/line, we can proceed using selected values if they exist
        if (resolvedBook == null || resolvedChapter == null) {
            // If command is purely page/para/line and there are selected values, proceed.
            if ((cmd.page != null || cmd.paragraph != null || cmd.line != null) && selectedBook.value != null && selectedChapter.value != null) {
                resolvedBook = selectedBook.value
                resolvedChapter = selectedChapter.value
            }
        }

        // If after all this we still lack book/chapter, prompt the user to say them (but not aggressively loop)
        if (resolvedBook == null) {
            tts.speak("Please tell me the book name.")
            return
        }
        if (resolvedChapter == null) {
            tts.speak("Please tell me the chapter name.")
            return
        }

        // Final numbers — use parsed values or UI defaults
        val finalPage = cmd.page ?: pageInput.value.toIntOrNull() ?: 1
        val finalPara = cmd.paragraph ?: paragraphInput.value.toIntOrNull() ?: 1
        val finalLine = cmd.line ?: lineInput.value.toIntOrNull() ?: 1

        tts.speak("Opening $resolvedBook, chapter $resolvedChapter, page $finalPage, paragraph $finalPara, line $finalLine.")
        onOpenReader(resolvedBook, resolvedChapter, finalPage, finalPara, finalLine)
    }
}
