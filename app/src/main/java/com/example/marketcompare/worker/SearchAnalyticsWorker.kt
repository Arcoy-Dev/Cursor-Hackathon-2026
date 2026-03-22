package com.example.marketcompare.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.marketcompare.AppContainer
import kotlinx.coroutines.flow.first

class SearchAnalyticsWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val container = AppContainer(applicationContext)
        val totalSearches = container.marketRepository.getTotalSearches()
        val session = container.authRepository.observeSession().first()

        val syncSuccess = if (session != null) {
            container.cloudSyncRepository.syncPendingData(session.token)
        } else {
            true
        }

        return if (totalSearches >= 0 && syncSuccess) Result.success() else Result.retry()
    }
}
