package com.example.marketcompare.model

enum class MatchLevel {
    EXACT,
    CLOSE,
    SIMILAR,
    NONE
}

data class ProductMatch(
    val source: Product,
    val candidate: Product,
    val level: MatchLevel,
    val score: Double
)

data class PriceComparisonItem(
    val product: Product,
    val priceDifferenceEuro: Double,
    val priceDifferencePercent: Double
)
