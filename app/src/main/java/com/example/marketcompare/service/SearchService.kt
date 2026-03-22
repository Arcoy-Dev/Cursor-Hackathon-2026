package com.example.marketcompare.service

import com.example.marketcompare.model.Product
import com.example.marketcompare.utils.ProductNormalizer
import com.example.marketcompare.utils.SimilarityUtils

class SearchService {
    fun search(
        query: String,
        products: List<Product>,
        limit: Int = 20
    ): List<Product> {
        val normalizedQuery = ProductNormalizer.normalizeProductName(query)
        if (normalizedQuery.isBlank()) return emptyList()
        val queryTerms = normalizedQuery.split(" ").filter { it.isNotBlank() }.toSet()

        return products
            .asSequence()
            .mapNotNull { product ->
                val score = scoreProduct(normalizedQuery, queryTerms, product)
                if (score <= 0.0) null else product to score
            }
            .sortedWith(
                compareByDescending<Pair<Product, Double>> { it.second }
                    .thenBy { it.first.price }
            )
            .take(limit)
            .map { it.first }
            .toList()
    }

    private fun scoreProduct(
        normalizedQuery: String,
        queryTerms: Set<String>,
        product: Product
    ): Double {
        var score = 0.0
        val normalizedName = product.normalizedName
        val normalizedSynonyms = product.synonyms.map { ProductNormalizer.normalizeProductName(it) }
        val tags = product.tags.map { it.lowercase() }

        val exactName = normalizedName == normalizedQuery
        val prefixName = normalizedName.startsWith(normalizedQuery)
        val containsName = normalizedName.contains(normalizedQuery)
        if (exactName) score += 3.0
        if (prefixName) score += 2.0
        else if (containsName) score += 1.3

        val similarity = SimilarityUtils.normalizedLevenshteinSimilarity(normalizedQuery, normalizedName)

        val synonymHit = normalizedSynonyms.any {
            it.contains(normalizedQuery) || SimilarityUtils.normalizedLevenshteinSimilarity(normalizedQuery, it) >= 0.45
        }
        if (synonymHit) score += 1.4

        val tokenHits = queryTerms.count { term ->
            if (term.length < 2) return@count false
            tags.any { it.contains(term) } || normalizedName.contains(term)
        }
        score += tokenHits * 0.4

        // Hard relevance gate so unrelated products cannot outrank the intended one.
        val similarityThreshold = when {
            normalizedQuery.length <= 4 -> 0.80
            normalizedQuery.length <= 7 -> 0.72
            else -> 0.62
        }
        val lexicalSignal = exactName || prefixName || containsName || synonymHit || tokenHits > 0
        if (!lexicalSignal && similarity < similarityThreshold) {
            return 0.0
        }
        if (similarity >= similarityThreshold) {
            score += similarity * 2.2
        }

        queryTerms.forEach { term ->
            if (term.length < 2) return@forEach
            if (tags.any { it.contains(term) }) score += 0.2
            if (normalizedName.contains(term)) score += 0.2
        }

        return score
    }
}
