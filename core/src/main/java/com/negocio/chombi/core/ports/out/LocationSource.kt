package com.negocio.chombi.core.ports.out

import com.negocio.chombi.core.domain.LocationSample
import kotlinx.coroutines.flow.Flow

interface LocationSource {
    fun stream(): Flow<LocationSample>
}
