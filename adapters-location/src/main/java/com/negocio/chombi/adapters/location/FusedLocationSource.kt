// adapters-location/src/main/java/com/negocio/chombi/adapters/location/FusedLocationSource.kt
package com.negocio.chombi.adapters.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.*
import com.negocio.chombi.core.domain.*
import com.negocio.chombi.core.ports.out.LocationSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FusedLocationSource(
    context: Context,

    private val driverId: DriverId,
    private val lineId: LineId,
    private val busId: BusId,
    private val intervalMs: Long = 1_000,
    private val minUpdateMs: Long = 1_000,
    private val minDistanceM: Float = 10f
) : LocationSource {

    private val fused = LocationServices.getFusedLocationProviderClient(context.applicationContext)

    @SuppressLint("MissingPermission")
    override fun stream(): Flow<LocationSample> = callbackFlow {
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(minUpdateMs)
            .setMinUpdateDistanceMeters(minDistanceM)
            .build()

        val cb = object : LocationCallback() {
            override fun onLocationResult(r: LocationResult) {
                val loc = r.lastLocation ?: return
                val speedKmh: Double? = loc.speed.takeIf { it.isFinite() }?.toDouble()?.times(3.6)
                val bearing: Float? = if (loc.hasBearing()) loc.bearing else null

                trySend(
                    LocationSample(
                        driverId = driverId,
                        lineId = lineId,
                        point = GeoPoint(loc.latitude, loc.longitude),
                        busId = busId,
                        speedKmh = speedKmh,
                        bearing = bearing,
                        timestampMillis = System.currentTimeMillis()
                    )
                )
            }
        }

        try {
            fused.requestLocationUpdates(req, cb, Looper.getMainLooper())
        } catch (se: SecurityException) {
            close(se)
            return@callbackFlow
        }
        awaitClose { fused.removeLocationUpdates(cb) }
    }
}
