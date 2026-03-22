package com.example.marketcompare.data.repo

import com.example.marketcompare.data.model.ProductOffer

interface InventoryDataSource {
    suspend fun search(query: String): List<ProductOffer>
    fun supermarkets(): Set<String>
    fun catalogProductNames(): List<String> = emptyList()
    fun catalogProductCategories(): Map<String, String> = emptyMap()
    fun suggestProducts(query: String, limit: Int = 12): List<String> = emptyList()
}
