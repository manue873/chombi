// :app/src/main/java/com/negocio/chombi/tracking/ServiceUtils.kt
package com.negocio.chombi.tracking

import android.app.ActivityManager
import android.content.Context

fun <T> isServiceRunning(context: Context, service: Class<T>): Boolean {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    @Suppress("DEPRECATION")
    return am.getRunningServices(Integer.MAX_VALUE).any { it.service.className == service.name }
}
