package com.example.marketcompare.data.sync

import com.example.marketcompare.data.db.HistoryDao
import com.example.marketcompare.data.network.SyncApi
import com.example.marketcompare.data.network.SyncComparisonDto
import com.example.marketcompare.data.network.SyncPayload
import com.example.marketcompare.data.network.SyncSearchDto

class CloudSyncRepository(
    private val historyDao: HistoryDao,
    private val syncApi: SyncApi
) {
    suspend fun syncPendingData(token: String): Boolean {
        return try {
            val searches = historyDao.getUnsyncedSearches()
            val comparisons = historyDao.getUnsyncedComparisons()
            if (searches.isEmpty() && comparisons.isEmpty()) return true

            val payload = SyncPayload(
                searches = searches.map {
                    SyncSearchDto(
                        id = it.id,
                        query = it.query,
                        timestamp = it.timestamp,
                        supermarkets = it.supermarkets,
                        resultCount = it.resultCount
                    )
                },
                comparisons = comparisons.map {
                    SyncComparisonDto(
                        id = it.id,
                        productName = it.productName,
                        cheapestPrice = it.cheapestPrice,
                        mostExpensivePrice = it.mostExpensivePrice,
                        difference = it.difference,
                        timestamp = it.timestamp
                    )
                }
            )

            syncApi.syncHistory("Bearer $token", payload)

            if (searches.isNotEmpty()) {
                historyDao.markSearchesSynced(searches.map { it.id })
            }
            if (comparisons.isNotEmpty()) {
                historyDao.markComparisonsSynced(comparisons.map { it.id })
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
