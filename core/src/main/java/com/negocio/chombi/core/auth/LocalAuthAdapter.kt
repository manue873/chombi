package com.negocio.chombi.auth

import com.negocio.chombi.core.domain.DriverId
import com.negocio.chombi.core.domain.BusId
import com.negocio.chombi.core.auth.*
import kotlinx.coroutines.delay
import java.util.UUID

/**
 * Demo:
 *  - driver1@demo.com / 123456 -> Driver D-001 asignado al bus BUS-10
 *  - driver2@demo.com / 123456 -> Driver D-002 sin bus asignado (bloquear tracking)
 *  - passenger@demo.com / 123456 -> Passenger
 */
class LocalAuthAdapter : AuthPort {
    private var current: UserProfile? = null

    override suspend fun currentUser(): UserProfile? = current

    override suspend fun signIn(email: String, password: String): UserProfile {
        delay(250)
        if (password != "123456") error("Credenciales inválidas")
        current = when (email.lowercase()) {
            "driver1@demo.com" -> UserProfile(
                id = UserId(UUID.randomUUID().toString()),
                email = email,
                role = Role.Driver,
                driverId = DriverId("D-001"),
                busId = BusId("BUS-10")          // ✅ asignado
            )
            "driver2@demo.com" -> UserProfile(
                id = UserId(UUID.randomUUID().toString()),
                email = email,
                role = Role.Driver,
                driverId = DriverId("D-002"),
                busId = null                     // ✅ sin asignación
            )
            "passenger@demo.com" -> UserProfile(
                id = UserId(UUID.randomUUID().toString()),
                email = email,
                role = Role.Passenger
            )
            else -> error("Usuario no registrado")
        }
        return current!!
    }

    override suspend fun signOut() { current = null }
}
