// :app/src/main/java/com/negocio/chombi/tracking/LocationForegroundService.kt
package com.negocio.chombi.tracking

import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.*
import com.negocio.chombi.ServiceLocator

class LocationForegroundService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var notifier: ServiceNotifier

    override fun onCreate() {
        super.onCreate()
        notifier = ServiceNotifier(this)
        startForeground(notifier.notificationId, notifier.build("Enviando ubicación..."))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch { ServiceLocator.trackingService.start() } // usa tu implementación actual
        return START_STICKY
    }

    override fun onDestroy() {
        scope.launch { ServiceLocator.trackingService.stop() }
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
