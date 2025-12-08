package com.example.talktile_05.services


object WordNumberConverter {

    private val map = mapOf(
        "zero" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4,
        "five" to 5, "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9,
        "ten" to 10, "eleven" to 11, "twelve" to 12, "thirteen" to 13,
        "fourteen" to 14, "fifteen" to 15, "twenty" to 20, "thirty" to 30,
        "forty" to 40, "fifty" to 50, "sixty" to 60, "seventy" to 70,
        "eighty" to 80, "ninety" to 90, "hundred" to 100
    )

    fun parseWords(input: String): Int? {
        val parts = input.lowercase().split(" ", "-")
        var total = 0
        var current = 0

        for (p in parts) {
            val n = map[p] ?: continue

            if (n == 100) {
                if (current == 0) current = 1
                current *= 100
            } else {
                current += n
            }
        }

        total += current
        return if (total == 0) null else total
    }
}
