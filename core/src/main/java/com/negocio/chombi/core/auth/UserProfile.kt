package com.negocio.chombi.core.auth

import com.negocio.chombi.core.domain.DriverId
import com.negocio.chombi.core.domain.BusId
data class UserProfile(
    val id: UserId,
    val email: String,
    val role: Role,
    val driverId: DriverId? = null,
    val busId: BusId? = null
)