package com.negocio.chombi.adapters.http

import com.negocio.chombi.core.domain.LocationSample
import com.negocio.chombi.core.ports.out.LocationPublisher

class RetrofitLocationPublisher(private val api: ApiService) : LocationPublisher {
    override suspend fun publish(s: LocationSample) {
        val dto = LocationDto(
            driverId = s.driverId.value,
            lineId   = s.lineId.value,
            lat      = s.point.lat,
            lng      = s.point.lng,
            speedKmh = s.speedKmh,
            bearing  = s.bearing,
            timestamp= s.timestampMillis,
            busId    = s.busId.value
        )
        api.sendLocation(dto)
    }
}
