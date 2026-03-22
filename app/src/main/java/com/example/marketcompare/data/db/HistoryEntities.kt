package com.example.marketcompare.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_records")
data class SearchRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val timestamp: Long,
    val supermarkets: Int,
    val resultCount: Int,
    val synced: Boolean = false
)

@Entity(tableName = "comparison_records")
data class ComparisonRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productName: String,
    val cheapestPrice: Double,
    val mostExpensivePrice: Double,
    val difference: Double,
    val timestamp: Long,
    val synced: Boolean = false
)
