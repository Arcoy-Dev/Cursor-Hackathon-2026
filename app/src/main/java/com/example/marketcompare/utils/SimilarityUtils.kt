package com.example.marketcompare.utils

import kotlin.math.max

object SimilarityUtils {
    /**
     * Returns normalized similarity in [0.0, 1.0], based on Levenshtein distance.
     */
    fun normalizedLevenshteinSimilarity(a: String, b: String): Double {
        if (a == b) return 1.0
        if (a.isBlank() || b.isBlank()) return 0.0
        val distance = levenshteinDistance(a, b)
        val longest = max(a.length, b.length).toDouble().coerceAtLeast(1.0)
        return (1.0 - (distance / longest)).coerceIn(0.0, 1.0)
    }

    fun levenshteinDistance(a: String, b: String): Int {
        val rows = a.length + 1
        val cols = b.length + 1
        val dp = Array(rows) { IntArray(cols) }

        for (i in 0 until rows) dp[i][0] = i
        for (j in 0 until cols) dp[0][j] = j

        for (i in 1 until rows) {
            for (j in 1 until cols) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,       // deletion
                    dp[i][j - 1] + 1,       // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        return dp[a.length][b.length]
    }
}
