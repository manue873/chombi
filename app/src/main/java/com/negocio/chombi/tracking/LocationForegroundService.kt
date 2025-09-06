// app/src/main/java/com/negocio/chombi/tracking/LocationForegroundService.kt
package com.negocio.chombi.tracking

import android.app.Notification
import android.app.Service
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import com.negocio.chombi.MainActivity
import com.negocio.chombi.R
import com.negocio.chombi.ServiceLocator
import kotlinx.coroutines.*

class LocationForegroundService : Service() {

    companion object {
        const val ACTION_START = "com.negocio.chombi.tracking.ACTION_START"
        const val ACTION_STOP  = "com.negocio.chombi.tracking.ACTION_STOP"

        fun start(context: Context) {
            val i = Intent(context, LocationForegroundService::class.java).setAction(ACTION_START)
            ContextCompat.startForegroundService(context, i)
        }
        fun stop(context: Context) {
            val i = Intent(context, LocationForegroundService::class.java).setAction(ACTION_STOP)
            context.startService(i)
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var notifier: ServiceNotifier

    // Estado simple para evitar dobles inicios/paradas
    @Volatile private var running = false

    override fun onCreate() {
        super.onCreate()
        notifier = ServiceNotifier(this) // crea canal y expone channelId + notificationId
        startForeground(notifier.notificationId, buildNotification("Enviando ubicación…"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                scope.launch { safeStopTracking() }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START, null -> {
                scope.launch { safeStartTracking() }
                return START_STICKY
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.launch { safeStopTracking() }
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /* ---------------- Lógica ---------------- */

    private suspend fun safeStartTracking() {
        if (running) return
        running = true
        try {
            updateNotification("Iniciando…")
            ServiceLocator.trackingService.start()
            setActive(true)
            updateNotification("Enviando ubicación…")
        } catch (t: Throwable) {
            running = false
            setActive(false)
            updateNotification("Error al iniciar: ${t.message ?: "desconocido"}")
        }
    }

    private suspend fun safeStopTracking() {
        if (!running) {
            setActive(false)
            return
        }
        try {
            updateNotification("Deteniendo…")
            ServiceLocator.trackingServiceStop.stop()   // ✅ corregido
        } catch (_: Throwable) {
            // no interrumpimos el shutdown
        } finally {
            running = false
            setActive(false)
            updateNotification("Detenido")
        }
    }

    /* ------------- Notificación ------------- */

    private fun buildNotification(contentText: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val contentPI = TaskStackBuilder.create(this)
            .addNextIntentWithParentStack(openIntent)
            .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this, LocationForegroundService::class.java).setAction(ACTION_STOP)
        val stopPI = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, notifier.channelId)
            .setSmallIcon(android.R.drawable.ic_menu_mapmode) // pon uno tuyo; temporalmente puedes usar android.R.drawable.ic_menu_mylocation
            .setContentTitle("Chombi")
            .setContentText(contentText)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setContentIntent(contentPI)
            .addAction(android.R.drawable.ic_secure, "Detener", stopPI) // usa drawables propios si quieres
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = NotificationManagerCompat.from(this)
        nm.notify(notifier.notificationId, buildNotification(text))
    }

    /* ------------- Persistencia de estado ------------- */

    private fun setActive(value: Boolean) {
        // Mismo esquema de DriverScreen (chombi_prefs / tracking_active)
        getSharedPreferences("chombi_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("tracking_active", value).apply()
    }
}
