package com.example.marketcompare.data.repo

import com.example.marketcompare.data.model.ProductOffer
import com.example.marketcompare.data.network.InventoryApi

class RemoteInventoryDataSource(
    private val api: InventoryApi
) : InventoryDataSource {
    override suspend fun search(query: String): List<ProductOffer> {
        return try {
            api.searchProducts(query).map {
                ProductOffer(
                    supermarket = it.supermarket,
                    name = it.name,
                    price = it.price,
                    category = "Groceries",
                    keywords = emptyList(),
                    brand = "Remote Market",
                    specialType = "Regular"
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override fun supermarkets(): Set<String> = emptySet()
}
