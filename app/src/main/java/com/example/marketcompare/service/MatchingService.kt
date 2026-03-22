package com.example.marketcompare.service

import com.example.marketcompare.model.MatchLevel
import com.example.marketcompare.model.Product
import com.example.marketcompare.model.ProductMatch
import com.example.marketcompare.utils.SimilarityUtils

class MatchingService {
    fun findMatches(
        product: Product,
        allProducts: List<Product>
    ): List<ProductMatch> {
        return allProducts
            .asSequence()
            .filter { it.id != product.id }
            .mapNotNull { candidate ->
                val level = classifyMatch(product, candidate)
                if (level == MatchLevel.NONE) return@mapNotNull null
                val score = calculateScore(product, candidate, level)
                ProductMatch(source = product, candidate = candidate, level = level, score = score)
            }
            .sortedWith(
                compareByDescending<ProductMatch> { it.level.rank() }
                    .thenByDescending { it.score }
                    .thenBy { it.candidate.price }
            )
            .toList()
    }

    fun classifyMatch(productA: Product, productB: Product): MatchLevel {
        // EXACT: same normalizedName + same quantity + same unit
        if (
            productA.normalizedName == productB.normalizedName &&
            productA.unit.equals(productB.unit, ignoreCase = true) &&
            kotlin.math.abs(productA.quantity - productB.quantity) < 0.0001
        ) {
            return MatchLevel.EXACT
        }

        val nameSimilarity =
            SimilarityUtils.normalizedLevenshteinSimilarity(productA.normalizedName, productB.normalizedName)
        val quantitySimilar = areQuantitiesSimilar(productA, productB, tolerancePercent = 10.0)

        // CLOSE: similar names + +-10% quantity + same subcategory
        if (nameSimilarity >= 0.72 && quantitySimilar && productA.subcategory == productB.subcategory) {
            return MatchLevel.CLOSE
        }

        // SIMILAR: same category/subcategory + overlapping tags/synonyms and moderate name similarity
        val sameGrouping = productA.category == productB.category || productA.subcategory == productB.subcategory
        val overlap = overlapScore(productA, productB)
        if (sameGrouping && (overlap >= 0.25 || nameSimilarity >= 0.40)) {
            return MatchLevel.SIMILAR
        }

        return MatchLevel.NONE
    }

    private fun calculateScore(a: Product, b: Product, level: MatchLevel): Double {
        val nameScore = SimilarityUtils.normalizedLevenshteinSimilarity(a.normalizedName, b.normalizedName)
        val overlap = overlapScore(a, b)
        val quantityScore = when {
            areQuantitiesSimilar(a, b, 0.0) -> 1.0
            areQuantitiesSimilar(a, b, 10.0) -> 0.8
            else -> 0.3
        }
        val base = when (level) {
            MatchLevel.EXACT -> 1.0
            MatchLevel.CLOSE -> 0.75
            MatchLevel.SIMILAR -> 0.5
            MatchLevel.NONE -> 0.0
        }
        return (base * 0.4) + (nameScore * 0.3) + (overlap * 0.2) + (quantityScore * 0.1)
    }

    private fun overlapScore(a: Product, b: Product): Double {
        val aTerms = (a.tags + a.synonyms).map { it.lowercase() }.toSet()
        val bTerms = (b.tags + b.synonyms).map { it.lowercase() }.toSet()
        if (aTerms.isEmpty() || bTerms.isEmpty()) return 0.0
        val intersection = aTerms.intersect(bTerms).size.toDouble()
        val union = aTerms.union(bTerms).size.toDouble().coerceAtLeast(1.0)
        return intersection / union
    }

    private fun areQuantitiesSimilar(a: Product, b: Product, tolerancePercent: Double): Boolean {
        if (!a.unit.equals(b.unit, ignoreCase = true)) return false
        if (a.quantity <= 0.0 || b.quantity <= 0.0) return false
        val tolerance = tolerancePercent / 100.0
        val relativeDiff = kotlin.math.abs(a.quantity - b.quantity) / a.quantity
        return relativeDiff <= tolerance
    }

    private fun MatchLevel.rank(): Int = when (this) {
        MatchLevel.EXACT -> 3
        MatchLevel.CLOSE -> 2
        MatchLevel.SIMILAR -> 1
        MatchLevel.NONE -> 0
    }
}
