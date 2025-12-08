package com.example.talktile_05.services

object ParagraphSplitter {

    fun splitIntoParagraphs(text: String): List<String> {
        return text.split("\n\n").map { it.trim() }.filter { it.isNotEmpty() }
    }
}
