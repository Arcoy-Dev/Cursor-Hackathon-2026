package com.example.marketcompare.data.network

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

data class SyncPayload(
    val searches: List<SyncSearchDto>,
    val comparisons: List<SyncComparisonDto>
)

data class SyncSearchDto(
    val id: Long,
    val query: String,
    val timestamp: Long,
    val supermarkets: Int,
    val resultCount: Int
)

data class SyncComparisonDto(
    val id: Long,
    val productName: String,
    val cheapestPrice: Double,
    val mostExpensivePrice: Double,
    val difference: Double,
    val timestamp: Long
)

interface SyncApi {
    @POST("analytics/sync")
    suspend fun syncHistory(
        @Header("Authorization") bearerToken: String,
        @Body payload: SyncPayload
    )
}
