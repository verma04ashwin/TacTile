package com.example.talktile_05.services

import android.util.Log

data class ParsedCommand(
    val action: Action,
    val book: String? = null,
    val chapter: String? = null,
    val page: Int? = null,
    val paragraph: Int? = null,
    val line: Int? = null
) {
    enum class Action {
        NEXT_PAGE, PREV_PAGE,
        NEXT_PARAGRAPH, PREV_PARAGRAPH,
        GO_TO_PAGE, GO_TO_PARAGRAPH, GO_TO_LINE,
        GO_TO_BOOK, GO_TO_CHAPTER,
        OPEN_MAP,
        WHERE_AM_I, BOOKMARK,
        PAUSE, RESUME,
        UNKNOWN
    }
}

class VoiceCommandParser {

    // convert tokens like "three" -> 3 using existing helper
    private fun toNumber(token: String): Int? {
        token.toIntOrNull()?.let { return it }
        return WordNumberConverter.parseWords(token)
    }

    /**
     * Try to strip filler words, find number tokens and capture book/chapter candidate phrases.
     *
     * Outputs:
     *  - page, paragraph, line (Int?)
     *  - book (String?) -- a candidate phrase which the caller should fuzzy-match against available books
     *  - chapter (String?) -- candidate phrase for chapter matching
     */
    fun parse(rawInput: String): ParsedCommand {
        val raw = rawInput.lowercase().trim()
        Log.d("VoiceParser", "rawInput='$raw'")

        // Fast exact keywords
        if ("open map" in raw || "show map" in raw) return ParsedCommand(ParsedCommand.Action.OPEN_MAP)
        if ("next paragraph" in raw || "next para" in raw) return ParsedCommand(ParsedCommand.Action.NEXT_PARAGRAPH)
        if ("prev paragraph" in raw || "previous paragraph" in raw || "prev para" in raw) return ParsedCommand(ParsedCommand.Action.PREV_PARAGRAPH)
        if ("next page" in raw) return ParsedCommand(ParsedCommand.Action.NEXT_PAGE)
        if ("prev page" in raw || "previous page" in raw) return ParsedCommand(ParsedCommand.Action.PREV_PAGE)
        if ("where am i" in raw) return ParsedCommand(ParsedCommand.Action.WHERE_AM_I)
        if ("pause" in raw) return ParsedCommand(ParsedCommand.Action.PAUSE)
        if ("resume" in raw || "continue" in raw) return ParsedCommand(ParsedCommand.Action.RESUME)
        if ("bookmark" in raw) return ParsedCommand(ParsedCommand.Action.BOOKMARK)

        // Normalize punctuation to spaces and split tokens
        val normalized = raw.replace(Regex("[^a-z0-9\\s-]"), " ")
        val tokens = normalized.split(Regex("\\s+")).filter { it.isNotBlank() }

        // Gather numeric tokens near keywords
        var page: Int? = null
        var paragraph: Int? = null
        var line: Int? = null

        // we will also build a list of non-number tokens excluding filler keywords and numbers
        val filler = setOf(
            "open","goto","go","to","in","on","the","a","please","show","read",
            "page","number","paragraph","para","line","chapter","book","of","for","and","my","this","that"
        )

        // two-pass: find numbers after keywords first (page 3, paragraph 2, line 4)
        for (i in tokens.indices) {
            val t = tokens[i]
            if (t == "page" || t == "pages") {
                val next = tokens.getOrNull(i + 1)
                if (next != null) toNumber(next)?.let { page = it }
            }
            if (t == "paragraph" || t == "para") {
                val next = tokens.getOrNull(i + 1)
                if (next != null) toNumber(next)?.let { paragraph = it }
            }
            if (t == "line") {
                val next = tokens.getOrNull(i + 1)
                if (next != null) toNumber(next)?.let { line = it }
            }
        }

        // second pass: capture standalone number tokens if still null (e.g., "page three" covered, or "three" alone)
        for (i in tokens.indices) {
            val t = tokens[i]
            val n = toNumber(t)
            if (n != null) {
                // heuristics: if we've seen "page" before use it, otherwise assign first numeric to page, then para, then line
                if (page == null && tokens.getOrNull(i-1) != "paragraph" && tokens.getOrNull(i-1) != "para" && tokens.getOrNull(i-1) != "line") {
                    page = n
                    continue
                }
                if (paragraph == null && (tokens.getOrNull(i-1) == "paragraph" || tokens.getOrNull(i-1) == "para")) {
                    paragraph = n; continue
                }
                if (line == null && tokens.getOrNull(i-1) == "line") {
                    line = n; continue
                }
                // fallback assignments
                if (page == null) { page = n; continue }
                if (paragraph == null) { paragraph = n; continue }
                if (line == null) { line = n; continue }
            }
        }

        // Build candidate book/chapter phrase by removing known words and numbers.
        val leftoverTokens = tokens.filter { t ->
            t !in filler && toNumber(t) == null
        }

        // If raw contains "chapter", prefer splitting by that keyword
        var bookCandidate: String? = null
        var chapterCandidate: String? = null

        if (raw.contains(" chapter ")) {
            val parts = raw.split(Regex("chapter"), limit = 2)
            val left = parts.getOrNull(0)?.trim() ?: ""
            val right = parts.getOrNull(1)?.trim() ?: ""
            if (left.isNotBlank()) {
                // left might include verbs; remove filler
                val leftTokens = left.split(Regex("\\s+")).filter { it.isNotBlank() && it !in filler && toNumber(it) == null }
                if (leftTokens.isNotEmpty()) bookCandidate = leftTokens.joinToString(" ")
            }
            if (right.isNotBlank()) {
                val rightTokens = right.split(Regex("\\s+")).filter { it.isNotBlank() && it !in filler && toNumber(it) == null }
                if (rightTokens.isNotEmpty()) chapterCandidate = rightTokens.joinToString(" ")
            }
        } else if (raw.contains(" book ")) {
            val parts = raw.split(Regex("book"), limit = 2)
            val right = parts.getOrNull(1)?.trim() ?: ""
            if (right.isNotBlank()) {
                val rightTokens = right.split(Regex("\\s+")).filter { it.isNotBlank() && it !in filler && toNumber(it) == null }
                if (rightTokens.isNotEmpty()) bookCandidate = rightTokens.joinToString(" ")
            } else {
                // left side maybe book name
                val left = parts.getOrNull(0)?.trim() ?: ""
                val leftTokens = left.split(Regex("\\s+")).filter { it.isNotBlank() && it !in filler && toNumber(it) == null }
                if (leftTokens.isNotEmpty()) bookCandidate = leftTokens.joinToString(" ")
            }
        } else {
            // fallback: try to heuristically assign leftover tokens to book or chapter
            if (leftoverTokens.isNotEmpty()) {
                val joined = leftoverTokens.joinToString(" ")
                // If leftover contains "and" or ampersand, try splitting
                val splitCandidates = joined.split(Regex(" and | & | - "))
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                if (splitCandidates.size >= 2) {
                    // first could be book, second chapter (best-effort)
                    bookCandidate = splitCandidates[0]
                    chapterCandidate = splitCandidates.drop(1).joinToString(" ")
                } else {
                    // single candidate — caller will attempt matching
                    bookCandidate = joined
                }
            }
        }

        // If nothing meaningful found, return UNKNOWN
        if (page == null && paragraph == null && line == null && bookCandidate == null && chapterCandidate == null) {
            return ParsedCommand(ParsedCommand.Action.UNKNOWN)
        }

        // If page present -> prefer GO_TO_PAGE with all optional fields included
        if (page != null) {
            return ParsedCommand(
                action = ParsedCommand.Action.GO_TO_PAGE,
                page = page,
                paragraph = paragraph,
                line = line,
                book = bookCandidate,
                chapter = chapterCandidate
            )
        }

        if (paragraph != null) {
            return ParsedCommand(
                action = ParsedCommand.Action.GO_TO_PARAGRAPH,
                paragraph = paragraph,
                book = bookCandidate,
                chapter = chapterCandidate
            )
        }

        if (line != null) {
            return ParsedCommand(
                action = ParsedCommand.Action.GO_TO_LINE,
                line = line,
                book = bookCandidate,
                chapter = chapterCandidate
            )
        }

        // fallbacks for book/chapter only requests
        if (bookCandidate != null && chapterCandidate != null) {
            return ParsedCommand(ParsedCommand.Action.GO_TO_BOOK, book = bookCandidate, chapter = chapterCandidate)
        }
        if (bookCandidate != null) {
            return ParsedCommand(ParsedCommand.Action.GO_TO_BOOK, book = bookCandidate)
        }
        if (chapterCandidate != null) {
            return ParsedCommand(ParsedCommand.Action.GO_TO_CHAPTER, chapter = chapterCandidate)
        }

        return ParsedCommand(ParsedCommand.Action.UNKNOWN)
    }
}
