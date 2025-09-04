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
    private val intervalMs: Long = 4_000,
    private val minUpdateMs: Long = 3_000,
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
                trySend(
                    LocationSample(
                        driverId,
                        lineId,
                        GeoPoint(loc.latitude, loc.longitude),
                        loc.speed.takeIf { it.isFinite() }?.times(3.6),
                        if (loc.hasBearing()) loc.bearing else null,
                        System.currentTimeMillis()
                    )
                )
            }
        }

        fused.requestLocationUpdates(req, cb, Looper.getMainLooper())
        awaitClose { fused.removeLocationUpdates(cb) }
    }
}
