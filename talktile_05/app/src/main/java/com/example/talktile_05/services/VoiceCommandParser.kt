package com.example.talktile_05.services

import java.util.regex.Pattern

data class ParsedCommand(
    val action: Action,
    val book: String? = null,
    val chapter: String? = null,
    val page: Int? = null,
    val paragraph: Int? = null
) {
    enum class Action {
        NEXT_PARAGRAPH, PREV_PARAGRAPH, NEXT_PAGE, PREV_PAGE,
        GO_TO_PAGE, GO_TO_PARAGRAPH, GO_TO_CHAPTER, GO_TO_BOOK,
        OPEN_MAP, // new
        PAUSE, RESUME, STOP, WHERE_AM_I, BOOKMARK, UNKNOWN
    }
}

class VoiceCommandParser {

    // -- small natural number helper (delegates to WordNumberConverter if needed)
    private val pageDigits = Pattern.compile("page\\s+(\\d+)")
    private val pageWords = Regex("page\\s+([a-z\\s-]+)")

    // ------------------------------------------------------------
// Fuzzy match (Levenshtein distance)
// ------------------------------------------------------------
    fun bestMatch(input: String, options: List<String>): String? {
        if (options.isEmpty()) return null
        val cleaned = input.lowercase()
        return options.minByOrNull { levenshtein(cleaned, it.lowercase()) }
    }

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in dp.indices) dp[i][0] = i
        for (j in dp[0].indices) dp[0][j] = j

        for (i in 1..a.length) {
            for (j in 1..b.length) {
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1
                )
            }
        }
        return dp[a.length][b.length]
    }

    fun parse(raw: String): ParsedCommand {
        val cmd = raw.lowercase().trim()

        if (cmd.contains("next paragraph") || cmd.contains("next para"))
            return ParsedCommand(ParsedCommand.Action.NEXT_PARAGRAPH)

        if (cmd.contains("previous paragraph") || cmd.contains("prev paragraph") || cmd.contains("previous para"))
            return ParsedCommand(ParsedCommand.Action.PREV_PARAGRAPH)

        if (cmd.contains("next page"))
            return ParsedCommand(ParsedCommand.Action.NEXT_PAGE)

        if (cmd.contains("previous page") || cmd.contains("prev page"))
            return ParsedCommand(ParsedCommand.Action.PREV_PAGE)

        if (cmd.contains("where am i"))
            return ParsedCommand(ParsedCommand.Action.WHERE_AM_I)

        if (cmd.contains("pause"))
            return ParsedCommand(ParsedCommand.Action.PAUSE)

        if (cmd.contains("resume") || cmd.contains("continue"))
            return ParsedCommand(ParsedCommand.Action.RESUME)

        if (cmd.contains("bookmark"))
            return ParsedCommand(ParsedCommand.Action.BOOKMARK)

        // OPEN MAP
        if (cmd.contains("open map") || cmd.contains("show map") || cmd.contains("open the map"))
            return ParsedCommand(ParsedCommand.Action.OPEN_MAP)

        // GO TO PAGE numeric
        pageDigits.matcher(cmd).let { m ->
            if (m.find()) {
                val p = m.group(1).toIntOrNull()
                if (p != null) return ParsedCommand(ParsedCommand.Action.GO_TO_PAGE, page = p)
            }
        }

        // GO TO PAGE words
        pageWords.find(cmd)?.groupValues?.get(1)?.trim()?.let { words ->
            WordNumberConverter.parseWords(words)?.let { num ->
                return ParsedCommand(ParsedCommand.Action.GO_TO_PAGE, page = num)
            }
        }

        // paragraph
        Regex("(?:paragraph|para)\\s+(\\d+)").find(cmd)?.groupValues?.get(1)?.toIntOrNull()?.let {
            return ParsedCommand(ParsedCommand.Action.GO_TO_PARAGRAPH, paragraph = it)
        }

        // chapter / book
        var chapter: String? = null
        var book: String? = null
        Regex("chapter\\s+([\\w\\s\\-&]+)").find(cmd)?.let { chapter = it.groupValues[1].trim() }
        Regex("book\\s+([\\w\\s\\-&]+)").find(cmd)?.let { book = it.groupValues[1].trim() }

        if (book != null && chapter != null)
            return ParsedCommand(ParsedCommand.Action.GO_TO_BOOK, book = book, chapter = chapter)

        if (book != null)
            return ParsedCommand(ParsedCommand.Action.GO_TO_BOOK, book = book)

        if (chapter != null)
            return ParsedCommand(ParsedCommand.Action.GO_TO_CHAPTER, chapter = chapter)

        return ParsedCommand(ParsedCommand.Action.UNKNOWN)
    }
}
