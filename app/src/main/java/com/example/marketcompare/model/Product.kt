package com.example.marketcompare.model

/**
 * Canonical product model for cross-market matching and pricing.
 *
 * normalizedName and pricePerUnit are precomputed to keep search/matching fast at runtime.
 */
data class Product(
    val id: String,
    val name: String,
    val brand: String?,
    val category: String,
    val subcategory: String,
    val quantity: Double,
    val unit: String,
    val price: Double,
    val currency: String,
    val supermarket: String,
    val country: String,
    val isBio: Boolean,
    val isVegan: Boolean,
    val isVegetarian: Boolean,
    val isLactoseFree: Boolean,
    val isGlutenFree: Boolean,
    val tags: List<String>,
    val synonyms: List<String>,
    val normalizedName: String,
    val pricePerUnit: Double,
    val imageUrl: String?,
    val inStock: Boolean
)
