package com.example.marketcompare.utils

object ProductNormalizer {
    private val stopWords = setOf(
        "frisch",
        "extra",
        "beste",
        "hausmarke",
        "wahl",
        "classic",
        "original"
    )

    private val quantityRegex = Regex(
        pattern = "\\b\\d+(?:[\\.,]\\d+)?\\s?(kg|g|mg|l|ml|cl|stück|stk|pack|x)\\b",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    /**
     * Example:
     * "REWE Beste Wahl Bio Vollmilch 1L" -> "bio vollmilch"
     */
    fun normalizeProductName(
        name: String,
        brandToIgnore: String? = null
    ): String {
        var normalized = name.lowercase()
            .replace("ä", "ae")
            .replace("ö", "oe")
            .replace("ü", "ue")
            .replace("ß", "ss")

        if (!brandToIgnore.isNullOrBlank()) {
            normalized = normalized.replace(Regex("\\b${Regex.escape(brandToIgnore.lowercase())}\\b"), " ")
        }

        normalized = quantityRegex.replace(normalized, " ")
        normalized = normalized.replace(Regex("[^a-z0-9\\s]"), " ")

        val tokens = normalized
            .split(Regex("\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filterNot { it in stopWords }

        return tokens.joinToString(" ").trim()
    }
}
