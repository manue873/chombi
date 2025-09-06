// app/src/main/java/com/negocio/chombi/ServiceLocator.kt
package com.negocio.chombi

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.negocio.chombi.adapters.location.FusedLocationSource
import com.negocio.chombi.core.domain.*
import com.negocio.chombi.core.ports.`in`.StartTrackingUseCase
import com.negocio.chombi.core.ports.`in`.StopTrackingUseCase
import com.negocio.chombi.core.ports.out.LocationPublisher
import com.negocio.chombi.adapters.http.ApiService
import com.negocio.chombi.adapters.http.HttpFactory
import com.negocio.chombi.adapters.http.HttpFactory.api
import com.negocio.chombi.adapters.http.LinesRepository
import com.negocio.chombi.auth.LocalAuthAdapter
import com.negocio.chombi.core.auth.AuthPort
import com.negocio.chombi.http.RetrofitLocationPublisher
import com.negocio.chombi.tracking.DriverContext
import com.negocio.chombi.tracking.TrackingServiceImpl
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object ServiceLocator {
    lateinit var auth: AuthPort
    lateinit var linesRepository: LinesRepository
        private set

    @Volatile private var currentDriverContext: DriverContext? = null
    lateinit var application: Application
        private set

    lateinit var trackingService: StartTrackingUseCase
        private set
    lateinit var trackingServiceStop: StopTrackingUseCase
        private set

    fun init(app: Application) {
        application = app
        auth = LocalAuthAdapter()
        linesRepository = LinesRepository(api("https://99029a63e8a5.ngrok-free.app/"))

        // En producción estos IDs vienen de login/selección:
        val driverId = DriverId("DRV123")
        val lineId   = LineId("L45")
        val busId    = BusId("BUS12")

        val source = FusedLocationSource(
            context = app,
            driverId = driverId,
            lineId = lineId,
            busId = busId,
            intervalMs = 4_000,
            minUpdateMs = 3_000,
            minDistanceM = 10f
        )

        val api = HttpFactory.api("https://99029a63e8a5.ngrok-free.app/")
        val publisher: LocationPublisher = RetrofitLocationPublisher(api)

        val isOnline: () -> Boolean = isOnline@{
            val cm = app.getSystemService(ConnectivityManager::class.java)
                ?: return@isOnline false
            val net = cm.activeNetwork ?: return@isOnline false
            val caps = cm.getNetworkCapabilities(net) ?: return@isOnline false

            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    (
                            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                            )
        }

        val tracking = TrackingServiceImpl(
            source = source,
            publisher = publisher,
            driverId = driverId,
            lineId = lineId,
            busId = busId,
            isOnline = isOnline
        )

        trackingService = tracking
        trackingServiceStop = tracking
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
