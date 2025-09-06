// :adapters-http/src/main/java/com/negocio/chombi/http/RetrofitLocationPublisher.kt
package com.negocio.chombi.http

import com.negocio.chombi.adapters.http.ApiService
import com.negocio.chombi.adapters.http.LocationDto
import com.negocio.chombi.core.domain.LocationSample
import com.negocio.chombi.core.ports.out.LocationPublisher
import java.io.IOException

class RetrofitLocationPublisher(
    private val api: ApiService
) : LocationPublisher {

    override suspend fun publish(sample: LocationSample) {
        val dto = LocationDto(
            driverId = sample.driverId.value,
            lineId = sample.lineId.value,
            busId = sample.busId.value,
            lat = sample.point.lat,
            lng = sample.point.lng,
            speedKmh = sample.speedKmh,
            bearing = sample.bearing,
            timestamp = sample.timestampMillis
        )
        try {
            api.sendLocation(dto) // lanza excepción en HTTP != 2xx si usas Result adapters, si no, valida el código
        } catch (e: IOException) {
            throw e // red, reintentable
        } catch (e: Exception) {
            // Si quieres discriminar 5xx, mapea aquí a HttpRetryableException
            throw e
        }
    }
}
