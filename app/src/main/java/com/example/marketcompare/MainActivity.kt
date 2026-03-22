package com.example.marketcompare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.marketcompare.ui.screens.MarketAppScreen
import com.example.marketcompare.ui.screens.MarketViewModel
import com.example.marketcompare.ui.theme.MarketCompareTheme
import com.example.marketcompare.worker.SearchAnalyticsWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val appContainer by lazy { AppContainer(this) }

    private val viewModel: MarketViewModel by viewModels {
        MarketViewModel.factory(
            repository = appContainer.marketRepository,
            authRepository = appContainer.authRepository,
            cloudSyncRepository = appContainer.cloudSyncRepository
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        scheduleAnalyticsWork()

        setContent {
            MarketCompareTheme {
                MarketAppScreen(viewModel = viewModel)
            }
        }
    }

    private fun scheduleAnalyticsWork() {
        val workRequest = PeriodicWorkRequestBuilder<SearchAnalyticsWorker>(12, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "search_analytics_worker",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
