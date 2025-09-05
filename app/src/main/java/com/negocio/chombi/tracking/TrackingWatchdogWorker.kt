// :app/src/main/java/com/negocio/chombi/tracking/TrackingWatchdogWorker.kt
package com.negocio.chombi.tracking

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class TrackingWatchdogWorker(appContext: Context, params: WorkerParameters)
    : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.Default) {
        val active = AppPrefs(applicationContext).isDriverActive()
        if (active && !isServiceRunning(applicationContext, LocationForegroundService::class.java)) {
            TrackingService.start(applicationContext)
        }
        Result.success()
    }

    companion object {
        private const val UNIQUE_NAME = "tracking_watchdog"

        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<TrackingWatchdogWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME, ExistingPeriodicWorkPolicy.UPDATE, req
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
        }
    }
}
