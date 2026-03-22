package com.example.marketcompare.data.network

import retrofit2.http.GET
import retrofit2.http.Query

data class RemoteOfferDto(
    val supermarket: String,
    val name: String,
    val price: Double
)

interface InventoryApi {
    @GET("inventory/search")
    suspend fun searchProducts(@Query("q") query: String): List<RemoteOfferDto>
}
