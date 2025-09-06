package com.negocio.chombi.tracking

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.negocio.chombi.R

class ServiceNotifier(private val ctx: Context) {
    val channelId = "tracking_channel"
    val notificationId = 1001

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                channelId,
                "Tracking en segundo plano",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Envío de ubicación en tiempo real" }
            (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(ch)
        }
    }

    fun build(content: String): Notification {
        // (Opcional) builder simple si no quieres acciones extras
        return NotificationCompat.Builder(ctx, channelId)
            .setContentTitle("Chombi")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_mapmode)
            .setOngoing(true)
            .build()
    }
}
