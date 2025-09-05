package com.negocio.chombi

import android.app.Application
import com.negocio.chombi.auth.LocalAuthAdapter
import com.negocio.chombi.core.auth.AuthPort
import com.negocio.chombi.tracking.DriverContext
import com.negocio.chombi.tracking.TrackingService

object ServiceLocator {
    lateinit var auth: AuthPort
    lateinit var trackingService: TrackingService

    // ✅ contexto en memoria para la sesión del conductor
    @Volatile private var currentDriverContext: DriverContext? = null

    fun init(app: Application) {
        auth = LocalAuthAdapter()
        // trackingService = ...
    }

    suspend fun refreshDriverContext() {
        val profile = auth.currentUser()

        val d = profile?.driverId   // ← copia local
        val b = profile?.busId      // ← copia local

        currentDriverContext = if (d != null && b != null) {
            DriverContext(d, b)
        } else {
            null
        }
    }

    fun getDriverContext(): DriverContext? = currentDriverContext
}
