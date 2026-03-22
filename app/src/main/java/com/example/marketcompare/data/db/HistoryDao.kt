package com.example.marketcompare.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.marketcompare.data.model.DailySearchCount
import com.example.marketcompare.data.model.DailySavingsPoint
import com.example.marketcompare.data.model.ProductSearchCount
import com.example.marketcompare.data.model.ProductSearchHourPoint
import com.example.marketcompare.data.model.ProductSearchTrendPoint
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert
    suspend fun insertSearchRecord(record: SearchRecordEntity)

    @Insert
    suspend fun insertComparisonRecords(records: List<ComparisonRecordEntity>)

    @Query(
        """
        SELECT date(timestamp / 1000, 'unixepoch') AS day, COUNT(*) AS count
        FROM search_records
        GROUP BY day
        ORDER BY day ASC
        """
    )
    fun observeSearchesPerDay(): Flow<List<DailySearchCount>>

    @Query(
        """
        SELECT date(timestamp / 1000, 'unixepoch') AS day, AVG(difference) AS averageSavings
        FROM comparison_records
        GROUP BY day
        ORDER BY day ASC
        """
    )
    fun observeAverageSavingsPerDay(): Flow<List<DailySavingsPoint>>

    @Query(
        """
        SELECT lower(query) AS product, COUNT(*) AS count
        FROM search_records
        GROUP BY lower(query)
        ORDER BY count DESC, product ASC
        LIMIT :limit
        """
    )
    fun observeTopSearchedProducts(limit: Int = 8): Flow<List<ProductSearchCount>>

    @Query(
        """
        SELECT date(timestamp / 1000, 'unixepoch') AS day, COUNT(*) AS count
        FROM search_records
        WHERE lower(query) = lower(:product)
        GROUP BY day
        ORDER BY day ASC
        """
    )
    fun observeProductSearchTrend(product: String): Flow<List<ProductSearchTrendPoint>>

    @Query(
        """
        SELECT strftime('%H:00', timestamp / 1000, 'unixepoch', 'localtime') AS hour, COUNT(*) AS count
        FROM search_records
        WHERE lower(query) = lower(:product)
        GROUP BY hour
        ORDER BY hour ASC
        """
    )
    fun observeProductSearchHours(product: String): Flow<List<ProductSearchHourPoint>>

    @Query(
        """
        SELECT query
        FROM search_records
        ORDER BY timestamp DESC
        LIMIT :limit
        """
    )
    fun observeRecentQueries(limit: Int = 8): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM search_records")
    suspend fun getSearchCount(): Int

    @Query("SELECT * FROM search_records WHERE synced = 0 ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getUnsyncedSearches(limit: Int = 100): List<SearchRecordEntity>

    @Query("SELECT * FROM comparison_records WHERE synced = 0 ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getUnsyncedComparisons(limit: Int = 400): List<ComparisonRecordEntity>

    @Query("UPDATE search_records SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSearchesSynced(ids: List<Long>)

    @Query("UPDATE comparison_records SET synced = 1 WHERE id IN (:ids)")
    suspend fun markComparisonsSynced(ids: List<Long>)
}
