// :app/src/main/java/com/negocio/chombi/tracking/ServiceNotifier.kt
package com.negocio.chombi.tracking

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.negocio.chombi.MainActivity
import com.negocio.chombi.R

class ServiceNotifier(private val ctx: Context) {
    val channelId = "tracking_channel"
    val notificationId = 1001

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(channelId, "Tracking", NotificationManager.IMPORTANCE_LOW)
            ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    fun build(text: String): Notification {
        val pending = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(ctx, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation) // o android.R.drawable.ic_menu_mylocation
            .setContentTitle("Chombi")
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(pending)
            .build()
    }
}
