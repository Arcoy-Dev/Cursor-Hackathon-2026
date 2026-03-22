package com.example.marketcompare.data.model

data class ProductOffer(
    val supermarket: String,
    val name: String,
    val price: Double,
    val category: String = "Groceries",
    val keywords: List<String> = emptyList(),
    val brand: String = "Generic",
    val specialType: String = "Regular",
    val packageAmount: String = "1 unit",
    val weeklySales: Int = 0
)

data class ProductComparison(
    val productName: String,
    val offers: List<ProductOffer>,
    val cheapest: ProductOffer?,
    val mostExpensive: ProductOffer?
)

data class DailySearchCount(
    val day: String,
    val count: Int
)

data class DailySavingsPoint(
    val day: String,
    val averageSavings: Double
)

data class ProductSearchCount(
    val product: String,
    val count: Int
)

data class ProductSearchTrendPoint(
    val day: String,
    val count: Int
)

data class ProductSearchHourPoint(
    val hour: String,
    val count: Int
)
