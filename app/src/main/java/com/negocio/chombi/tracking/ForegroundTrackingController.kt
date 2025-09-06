// :app/src/main/java/com/negocio/chombi/tracking/TrackingService.kt
package com.negocio.chombi.tracking

import android.content.Context
import android.os.Build
import com.negocio.chombi.prefts.AppPrefs

object ForegroundTrackingController {

    fun start(ctx: Context) {
        // marca estado
        AppPrefs(ctx).setDriverActive(true)

        // arranca el Foreground Service
        val i = android.content.Intent(ctx, LocationForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i)
        else ctx.startService(i)

        // agenda watchdog
        TrackingWatchdogWorker.schedule(ctx)
    }

    fun stop(ctx: Context) {
        // apaga servicio
        ctx.stopService(android.content.Intent(ctx, LocationForegroundService::class.java))

        // limpia estado y watchdog
        AppPrefs(ctx).setDriverActive(false)
        TrackingWatchdogWorker.cancel(ctx)
    }
}
