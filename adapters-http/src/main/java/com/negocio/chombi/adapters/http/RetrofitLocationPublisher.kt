package com.negocio.chombi.adapters.http

import com.negocio.chombi.core.domain.LocationSample
import com.negocio.chombi.core.ports.out.LocationPublisher

class RetrofitLocationPublisher(private val api: ApiService) : LocationPublisher {
    override suspend fun publish(s: LocationSample) {
        api.sendLocation(
            LocationDto(
                s.driverId.value, s.lineId.value,
                s.point.lat, s.point.lng,
                s.speedKmh, s.bearing, s.timestampMillis
            )
        )
    }
}
