package com.example.talktile_05.viewmodel

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.talktile_05.App
import com.example.talktile_05.data.PdfRepository
import com.example.talktile_05.data.db.AppDatabase
import com.example.talktile_05.data.db.Bookmark
import com.example.talktile_05.data.db.ReadingState
import com.example.talktile_05.services.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class ReaderViewModel(
    private val repo: PdfRepository = PdfRepository(),
    private val extractor: PdfTextExtractor = PdfTextExtractor(App.instance),
    private val tts: TtsManager = TtsManager(App.instance),
    private val stt: SttManager = SttManager(),
    private val parser: VoiceCommandParser = VoiceCommandParser(),
    private val mapLoader: MapMetadataLoader = MapMetadataLoader(App.instance)
) : ViewModel() {

    val book = MutableStateFlow<String?>(null)
    val chapter = MutableStateFlow<String?>(null)
    val currentPage = MutableStateFlow(1)
    val currentParagraphIndex = MutableStateFlow(0)

    private val _paragraphs = MutableStateFlow(emptyList<String>())
    val pageBitmap = mutableStateOf<Bitmap?>(null)
    val isLoading = MutableStateFlow(false)

    private var mapInfoList: List<MapInfo> = emptyList()

    // UI layer collects this to navigate into map screen
    val openMapRequest = MutableStateFlow<String?>(null)

    val currentParagraphText = currentParagraphIndex
        .combine(_paragraphs) { idx, list -> list.getOrNull(idx) ?: "" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    private val db = AppDatabase.getInstance(App.instance)
    private val stateDao = db.readingStateDao()
    private val bookmarkDao = db.bookmarkDao()

    // ------------------------------------------------------------
    fun open(bookName: String, chapterName: String, page: Int) {
        book.value = bookName
        chapter.value = chapterName
        currentPage.value = page
        currentParagraphIndex.value = 0

        mapInfoList = mapLoader.loadMapInfo(bookName, chapterName)

        loadCurrentPage(firstOpen = true)
    }

    // ------------------------------------------------------------
    fun loadCurrentPage(firstOpen: Boolean = false) {
        val b = book.value ?: return
        val c = chapter.value ?: return
        val p = currentPage.value

        viewModelScope.launch {
            isLoading.value = true
            try {
                val pdfPath = repo.pdfPathFor(b, c)

                val paras = extractor.extractParagraphsFromAsset(pdfPath, p - 1)
                _paragraphs.value = paras.ifEmpty {
                    listOf("This page contains no readable text.")
                }

                currentParagraphIndex.value =
                    currentParagraphIndex.value.coerceIn(0, _paragraphs.value.lastIndex)

                pageBitmap.value = renderPageBitmap(pdfPath, p - 1)

                // ----- Check map presence -----
                val mapOnPage = mapInfoList.find { it.page == p }
                if (mapOnPage != null) {
                    tts.speak("This page contains a map: ${mapOnPage.name}. Say 'open map' to view it.")
                    openMapRequest.value = mapOnPage.name      // ★★★ IMPORTANT ★★★
                } else {
                    openMapRequest.value = null
                }

                delay(120)
                speakCurrent()

                stateDao.upsert(
                    ReadingState(
                        id = 1,
                        book = b,
                        chapter = c,
                        page = p,
                        paragraphIndex = currentParagraphIndex.value
                    )
                )

            } catch (e: Exception) {
                Log.e("ReaderVM", "Error loading page", e)
                tts.speak("Unable to load page.")
            } finally {
                isLoading.value = false
            }
        }
    }

    // ------------------------------------------------------------
    private fun renderPageBitmap(assetPath: String, index: Int): Bitmap? {
        return try {
            val cacheFile = File(App.instance.cacheDir, assetPath.replace("/", "_"))
            if (!cacheFile.exists()) {
                App.instance.assets.open(assetPath).use { input ->
                    FileOutputStream(cacheFile).use { out -> input.copyTo(out) }
                }
            }

            val fd = ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            val page = renderer.openPage(index)

            val bmp = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            page.close()
            renderer.close()
            bmp
        } catch (e: Exception) {
            Log.e("ReaderVM", "Render error: ${e.message}")
            null
        }
    }

    // ------------------------------------------------------------
    fun nextParagraph() {
        if (currentParagraphIndex.value < _paragraphs.value.lastIndex) {
            currentParagraphIndex.value++
            speakCurrent()
        } else nextPage()
    }

    fun prevParagraph() {
        if (currentParagraphIndex.value > 0) {
            currentParagraphIndex.value--
            speakCurrent()
        } else prevPage()
    }

    // ------------------------------------------------------------
    fun nextPage() {
        currentPage.value++
        currentParagraphIndex.value = 0
        loadCurrentPage()
    }

    fun prevPage() {
        if (currentPage.value > 1) {
            currentPage.value--
            currentParagraphIndex.value = 0
            loadCurrentPage()
        } else tts.speak("Already on the first page.")
    }

    // ------------------------------------------------------------
    private fun speakCurrent() {
        val text = currentParagraphText.value
        if (text.isNotBlank()) {
            tts.speak("Paragraph ${currentParagraphIndex.value + 1}. $text")
        }
    }

    fun pauseTTS() = tts.pause()
    fun resumeTTS() = tts.resume()

    // ------------------------------------------------------------
    fun startVoiceCommand(activity: Activity) {
        stt.startListening(activity) { spoken ->
            viewModelScope.launch { handleVoice(spoken) }
        }
    }

    private suspend fun handleVoice(raw: String) {
        val cmd = parser.parse(raw)

        when (cmd.action) {

            ParsedCommand.Action.NEXT_PARAGRAPH -> nextParagraph()
            ParsedCommand.Action.PREV_PARAGRAPH -> prevParagraph()
            ParsedCommand.Action.NEXT_PAGE -> nextPage()
            ParsedCommand.Action.PREV_PAGE -> prevPage()

            ParsedCommand.Action.GO_TO_PAGE -> cmd.page?.let {
                currentPage.value = it
                currentParagraphIndex.value = 0
                loadCurrentPage()
            }

            ParsedCommand.Action.GO_TO_PARAGRAPH -> {
                val idx = (cmd.paragraph ?: 1) - 1
                currentParagraphIndex.value =
                    idx.coerceIn(0, _paragraphs.value.lastIndex)
                speakCurrent()
            }

            ParsedCommand.Action.OPEN_MAP -> {
                val page = currentPage.value
                val map = mapInfoList.find { it.page == page }
                if (map != null) {
                    openMapRequest.value = map.name
                } else {
                    tts.speak("No map found on this page.")
                }
            }

            ParsedCommand.Action.WHERE_AM_I ->
                tts.speak("You are in ${book.value}, chapter ${chapter.value}, page ${currentPage.value}, paragraph ${currentParagraphIndex.value + 1}")

            ParsedCommand.Action.BOOKMARK -> {
                val b = book.value
                val c = chapter.value
                if (b != null && c != null) {
                    bookmarkDao.insert(
                        Bookmark(
                            book = b,
                            chapter = c,
                            page = currentPage.value,
                            paragraphIndex = currentParagraphIndex.value
                        )
                    )
                    tts.speak("Bookmarked.")
                }
            }

            else -> tts.speak("Command not recognized.")
        }
    }

    // ------------------------------------------------------------
    override fun onCleared() {
        super.onCleared()
        stt.destroy()
        tts.shutdown()
    }
}
