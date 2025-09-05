package com.negocio.chombi.tracking

import com.negocio.chombi.core.domain.DriverId
import com.negocio.chombi.core.domain.BusId

data class DriverContext(
    val driverId: DriverId,
    val busId: BusId
)
