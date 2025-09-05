package com.negocio.chombi.tracking

import android.content.Context
import android.content.Intent
import android.os.Build

fun Context.startTrackingService() {
    val i = Intent(this, LocationForegroundService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i) else startService(i)
}
fun Context.stopTrackingService() {
    stopService(Intent(this, LocationForegroundService::class.java))
}
