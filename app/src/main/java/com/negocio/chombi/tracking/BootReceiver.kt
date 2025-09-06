// :app/src/main/java/com/negocio/chombi/tracking/BootReceiver.kt
package com.negocio.chombi.tracking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.negocio.chombi.prefts.AppPrefs

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (AppPrefs(context).isDriverActive()) {
            ForegroundTrackingController.start(context)
        }
    }
}
