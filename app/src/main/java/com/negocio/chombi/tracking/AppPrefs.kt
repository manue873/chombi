// :app/src/main/java/com/negocio/chombi/tracking/AppPrefs.kt
package com.negocio.chombi.tracking

import android.content.Context
import androidx.core.content.edit

class AppPrefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("chombi_prefs", Context.MODE_PRIVATE)
    fun setDriverActive(active: Boolean) = sp.edit { putBoolean("driver_active", active) }
    fun isDriverActive(): Boolean = sp.getBoolean("driver_active", false)
}
