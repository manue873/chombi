package com.negocio.chombi.adapters.http

data class LocationDto(
    val driverId: String,
    val lineId: String,
    val lat: Double,
    val lng: Double,
    val speedKmh: Double?,
    val bearing: Float?,
    val timestamp: Long
)
