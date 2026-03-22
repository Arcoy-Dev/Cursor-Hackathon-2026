package com.example.marketcompare.service

import com.example.marketcompare.model.PriceComparisonItem
import com.example.marketcompare.model.Product

class PricingService {
    fun calculatePricePerUnit(price: Double, quantity: Double): Double {
        if (quantity <= 0.0) return price
        return price / quantity
    }

    fun findCheapestAlternative(
        source: Product,
        candidates: List<Product>
    ): Product? {
        return candidates
            .filter { it.id != source.id }
            .minByOrNull { it.price }
    }

    fun comparePricesAcrossSupermarkets(
        source: Product,
        matches: List<Product>
    ): List<PriceComparisonItem> {
        return matches
            .filter { it.id != source.id }
            .sortedBy { it.price }
            .map { candidate ->
                val euroDiff = candidate.price - source.price
                val percent = if (source.price > 0) {
                    (euroDiff / source.price) * 100.0
                } else {
                    0.0
                }
                PriceComparisonItem(
                    product = candidate,
                    priceDifferenceEuro = euroDiff,
                    priceDifferencePercent = percent
                )
            }
    }
}
