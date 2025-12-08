package com.example.talktile_05.services

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import java.util.Locale

class TtsManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var ready = false
    private var lastText = ""

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
            ready = true
        }
    }

    fun speak(text: String) {
        lastText = text

        if (ready) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ID")
            return
        }

        Handler(Looper.getMainLooper()).postDelayed({
            if (ready) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ID")
        }, 200)
    }

    fun pause() {
        tts?.stop()
    }

    fun resume() {
        if (lastText.isNotBlank()) speak(lastText)
    }

    fun shutdown() {
        tts?.shutdown()
    }
}
