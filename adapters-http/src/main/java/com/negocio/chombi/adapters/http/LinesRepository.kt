// adapters-http/src/main/java/com/negocio/chombi/adapters/http/LinesRepository.kt
package com.negocio.chombi.adapters.http

class LinesRepository(private val api: ApiService) {
    suspend fun latestByLine(lineId: String): List<LocationReadDto> {
        val res = api.latestByLine(lineId)
        return if (res.isSuccessful) res.body().orEmpty() else emptyList()
    }
    suspend fun getLines(): List<LineDto> {
        val res = api.getLines()
        return if (res.isSuccessful) res.body().orEmpty() else emptyList()
    }
    suspend fun getLineShape(lineId: String): List<LatLngDto> {        // ← NUEVO
        val res = api.getLineShape(lineId)
        return if (res.isSuccessful) res.body().orEmpty() else emptyList()
    }
}
