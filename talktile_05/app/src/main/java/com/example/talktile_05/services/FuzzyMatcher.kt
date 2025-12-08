package com.example.talktile_05.services

import kotlin.math.min

object FuzzyMatcher {

    fun bestMatch(input: String, options: List<String>): String? {
        if (options.isEmpty()) return null
        val cleaned = input.lowercase().trim()

        return options.minByOrNull { distance(cleaned, it.lowercase()) }
    }

    private fun distance(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }

        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j

        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[a.length][b.length]
    }
}
