package com.example.marketcompare.data

import com.example.marketcompare.model.MatchLevel
import com.example.marketcompare.service.MatchingService
import com.example.marketcompare.service.PricingService
import com.example.marketcompare.service.SearchService

/**
 * Simple executable-like demo checks.
 *
 * Can be called from tests or temporary debug entry points:
 * `MatchingSystemDemo.runDemoChecks()`
 */
object MatchingSystemDemo {
    fun runDemoChecks() {
        val products = SampleProducts.products
        check(products.size >= 100) { "Expected >= 100 products, got ${products.size}" }

        val searchService = SearchService()
        val matchingService = MatchingService()
        val pricingService = PricingService()

        // Fuzzy + synonym examples:
        val haferHits = searchService.search("Hafermilch", products, limit = 5)
        check(haferHits.any { it.name.contains("Hafer", ignoreCase = true) || it.synonyms.any { s -> s.contains("hafer", true) } })

        val tomatenHits = searchService.search("Tomatensosse", products, limit = 5)
        check(tomatenHits.any { it.name.contains("Tomaten", ignoreCase = true) || it.synonyms.any { s -> s.contains("tomatensosse", true) } })

        val milch = products.first { it.normalizedName.contains("vollmilch") }
        val matches = matchingService.findMatches(milch, products)
        check(matches.isNotEmpty()) { "Expected matches for Vollmilch." }
        check(matches.any { it.level == MatchLevel.EXACT || it.level == MatchLevel.CLOSE })

        val cheapest = pricingService.findCheapestAlternative(milch, matches.map { it.candidate })
        check(cheapest != null) { "Expected cheapest alternative." }

        val comparisons = pricingService.comparePricesAcrossSupermarkets(milch, matches.map { it.candidate })
        check(comparisons.isNotEmpty()) { "Expected price comparison entries." }
    }
}
