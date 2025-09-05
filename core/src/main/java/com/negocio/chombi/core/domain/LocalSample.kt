package com.negocio.chombi.core.domain

import com.negocio.chombi.core.domain.DriverId

data class LocationSample(
    val driverId: DriverId,
    val lineId: LineId,
    val point: GeoPoint,
    val busId: BusId,
    val speedKmh: Double?,
    val bearing: Float?,
    val timestampMillis: Long
)
