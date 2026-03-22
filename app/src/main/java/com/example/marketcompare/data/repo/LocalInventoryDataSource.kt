package com.example.marketcompare.data.repo

import com.example.marketcompare.data.SampleProducts
import com.example.marketcompare.data.model.ProductOffer
import com.example.marketcompare.model.MatchLevel
import com.example.marketcompare.model.Product
import com.example.marketcompare.service.MatchingService
import com.example.marketcompare.service.SearchService
import kotlin.math.absoluteValue

/**
 * Local data source backed by the new Product/Matching/Search MVP system.
 *
 * This keeps the existing app contract (ProductOffer) while using:
 * - structured Product data (SampleProducts)
 * - fuzzy/synonym search (SearchService)
 * - EXACT/CLOSE/SIMILAR expansion (MatchingService)
 */
class LocalInventoryDataSource : InventoryDataSource {
    private val searchService = SearchService()
    private val matchingService = MatchingService()

    private val allProducts: List<Product> by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        SampleProducts.products.filter { it.inStock }
    }

    private val supermarketInventory: Map<String, List<ProductOffer>> by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        allProducts
            .groupBy { it.supermarket }
            .mapValues { (_, products) ->
                products.map { it.toOffer(MatchLevel.SIMILAR) }
                    .sortedWith(compareBy<ProductOffer> { it.name.lowercase() }.thenBy { it.price })
            }
    }

    private val catalogProducts: List<Product> by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        allProducts.distinctBy { it.normalizedName }
    }

    override suspend fun search(query: String): List<ProductOffer> {
        val trimmed = query.trim()
        if (trimmed.length < 2) return emptyList()
        val queryLower = trimmed.lowercase()
        val queryTokens = queryLower.split(Regex("\\s+")).filter { it.isNotBlank() }

        val baseHits = searchService.search(
            query = trimmed,
            products = allProducts,
            limit = 120
        )
        if (baseHits.isEmpty()) return emptyList()

        // Expand around top results to include close/similar alternatives.
        val expanded = linkedMapOf<String, Pair<Product, MatchLevel>>()
        baseHits.forEach { product ->
            expanded[product.id] = product to lexicalMatchLevel(product, queryLower)
        }

        baseHits.take(6).forEach { seed ->
            val matches = matchingService.findMatches(seed, allProducts)
                .filter { it.level == MatchLevel.EXACT || it.level == MatchLevel.CLOSE || it.level == MatchLevel.SIMILAR }
                .take(36)
            matches.forEach { match ->
                val existing = expanded[match.candidate.id]
                if (existing == null || match.level.rank() > existing.second.rank()) {
                    expanded[match.candidate.id] = match.candidate to match.level
                }
            }
        }

        return expanded.values
            .asSequence()
            .map { (product, level) ->
                val score = relevanceScore(
                    product = product,
                    level = level,
                    queryLower = queryLower,
                    queryTokens = queryTokens
                )
                product.toOffer(level) to score
            }
            .sortedWith(
                compareByDescending<Pair<ProductOffer, Int>> { it.second }
                    .thenBy { it.first.price }
                    .thenBy { it.first.name.lowercase() }
                    .thenBy { it.first.supermarket.lowercase() }
            )
            .map { it.first }
            .take(1_200)
            .toList()
    }

    override fun suggestProducts(query: String, limit: Int): List<String> {
        val trimmed = query.trim()
        if (trimmed.length < 2) return emptyList()

        return searchService.search(
            query = trimmed,
            products = catalogProducts,
            limit = limit.coerceIn(1, 12) * 3
        )
            .map { it.name }
            .distinct()
            .take(limit.coerceIn(1, 12))
    }

    override fun supermarkets(): Set<String> = supermarketInventory.keys

    override fun catalogProductNames(): List<String> {
        return catalogProducts.map { it.name }.distinct().sorted()
    }

    override fun catalogProductCategories(): Map<String, String> {
        return catalogProducts.associate { it.name to it.category }
    }

    private fun Product.toOffer(matchLevel: MatchLevel): ProductOffer {
        val specialType = when {
            isBio -> "Bio"
            isVegan -> "Vegan"
            isGlutenFree -> "Gluten Free"
            isLactoseFree -> "Lactose Free"
            else -> "Regular"
        }
        val matchKeyword = when (matchLevel) {
            MatchLevel.EXACT -> "match_level_exact"
            MatchLevel.CLOSE -> "match_level_close"
            MatchLevel.SIMILAR -> "match_level_similar"
            MatchLevel.NONE -> "match_level_none"
        }

        return ProductOffer(
            supermarket = supermarket,
            name = name,
            price = price,
            category = category,
            keywords = (tags + synonyms + listOf("subcategory:${subcategory.lowercase()}", normalizedName, matchKeyword)).distinct(),
            brand = brand ?: "Generic",
            specialType = specialType,
            packageAmount = formatPackageAmount(quantity, unit),
            weeklySales = estimatedWeeklySales(this)
        )
    }

    private fun formatPackageAmount(quantity: Double, unit: String): String {
        val value = if (quantity % 1.0 == 0.0) quantity.toInt().toString() else quantity.toString()
        return "$value$unit".lowercase()
    }

    private fun estimatedWeeklySales(product: Product): Int {
        val base = when (product.category) {
            "Milchprodukte", "Obst", "Gemüse", "Backwaren", "Getränke", "Trockenwaren" -> 160
            "Fleisch & Fisch", "Vegane Alternativen", "Tiefkühlprodukte" -> 125
            "Snacks", "Konserven" -> 110
            else -> 95
        }
        val variance = (product.id.hashCode() + product.supermarket.hashCode()).absoluteValue % 60
        return (base + variance).coerceAtLeast(40)
    }

    private fun MatchLevel.rank(): Int = when (this) {
        MatchLevel.EXACT -> 3
        MatchLevel.CLOSE -> 2
        MatchLevel.SIMILAR -> 1
        MatchLevel.NONE -> 0
    }

    private fun lexicalMatchLevel(product: Product, queryLower: String): MatchLevel {
        val normalizedQuery = com.example.marketcompare.utils.ProductNormalizer.normalizeProductName(queryLower)
        if (normalizedQuery.isBlank()) return MatchLevel.SIMILAR

        val name = product.normalizedName.lowercase()
        val synonyms = product.synonyms.map {
            com.example.marketcompare.utils.ProductNormalizer.normalizeProductName(it).lowercase()
        }

        val exact = name == normalizedQuery || synonyms.any { it == normalizedQuery }
        if (exact) return MatchLevel.EXACT

        val close = name.startsWith(normalizedQuery) ||
            name.contains(normalizedQuery) ||
            synonyms.any { it.contains(normalizedQuery) }
        if (close) return MatchLevel.CLOSE

        return MatchLevel.SIMILAR
    }

    private fun relevanceScore(
        product: Product,
        level: MatchLevel,
        queryLower: String,
        queryTokens: List<String>
    ): Int {
        val nameLower = product.name.lowercase()
        val normalizedLower = product.normalizedName.lowercase()
        val synonymsLower = product.synonyms.map { it.lowercase() }
        val tagsLower = product.tags.map { it.lowercase() }

        var score = 0

        // Strong lexical intent first (prevents random-looking ordering).
        when {
            nameLower == queryLower || normalizedLower == queryLower -> score += 1200
            nameLower.startsWith(queryLower) || normalizedLower.startsWith(queryLower) -> score += 900
            nameLower.contains(queryLower) || normalizedLower.contains(queryLower) -> score += 650
        }

        if (synonymsLower.any { it == queryLower }) score += 520
        if (synonymsLower.any { it.startsWith(queryLower) }) score += 340
        if (synonymsLower.any { it.contains(queryLower) }) score += 220
        if (tagsLower.any { it == queryLower }) score += 180
        if (tagsLower.any { it.contains(queryLower) }) score += 120

        val tokenHits = queryTokens.count { token ->
            nameLower.contains(token) ||
                normalizedLower.contains(token) ||
                synonymsLower.any { it.contains(token) } ||
                tagsLower.any { it.contains(token) }
        }
        score += tokenHits * 110

        // MatchLevel still matters, but should not overpower lexical fit.
        score += when (level) {
            MatchLevel.EXACT -> 260
            MatchLevel.CLOSE -> 140
            MatchLevel.SIMILAR -> 60
            MatchLevel.NONE -> 0
        }

        return score
    }
}
