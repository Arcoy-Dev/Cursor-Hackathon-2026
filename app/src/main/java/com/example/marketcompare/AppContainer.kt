package com.example.marketcompare

import android.content.Context
import com.example.marketcompare.data.auth.AuthRepository
import com.example.marketcompare.data.db.AppDatabase
import com.example.marketcompare.data.network.NetworkModule
import com.example.marketcompare.data.repo.LocalInventoryDataSource
import com.example.marketcompare.data.repo.MarketRepository
import com.example.marketcompare.data.repo.RemoteInventoryDataSource
import com.example.marketcompare.data.sync.CloudSyncRepository

class AppContainer(context: Context) {
    private val db = AppDatabase.getInstance(context)

    val authRepository = AuthRepository(context)
    val marketRepository = MarketRepository(
        historyDao = db.historyDao(),
        localDataSource = LocalInventoryDataSource(),
        remoteDataSource = RemoteInventoryDataSource(NetworkModule.inventoryApi)
    )
    val cloudSyncRepository = CloudSyncRepository(
        historyDao = db.historyDao(),
        syncApi = NetworkModule.syncApi
    )
}
