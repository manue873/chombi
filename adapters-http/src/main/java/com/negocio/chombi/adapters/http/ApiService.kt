package com.negocio.chombi.adapters.http
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @POST("/v1/locations")
    suspend fun sendLocation(@Body body: LocationDto)
    @GET("/v1/lines/{lineId}/latest")
    suspend fun latestByLine(@Path("lineId") lineId: String): List<LocationReadDto>
}
