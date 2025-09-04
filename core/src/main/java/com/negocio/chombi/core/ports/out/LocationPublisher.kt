package com.negocio.chombi.core.ports.out

import com.negocio.chombi.core.domain.LocationSample

interface LocationPublisher {
    suspend fun publish(sample: LocationSample)
}
