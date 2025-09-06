package com.negocio.chombi.adapters.http

import com.google.gson.annotations.SerializedName

data class LocationReadDto(
    val id: Int,
    val driverId: String,
    val lineId: String,
    val lat: Double,
    val lng: Double,
    val speedKmh: Double?,
    val bearing: Float?,
    @SerializedName("timestampMillis")
    val timestamp: Long,
    val busId: String
)