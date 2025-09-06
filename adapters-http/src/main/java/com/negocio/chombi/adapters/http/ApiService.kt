package com.negocio.chombi.adapters.http

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @POST("v1/locations")
    suspend fun sendLocation(@Body body: LocationDto): Response<Unit>

    @GET("v1/lines/{lineId}/latest")
    suspend fun latestByLine(@Path("lineId") lineId: String): Response<List<LocationReadDto>>

    @GET("v1/lines")
    suspend fun getLines(): Response<List<LineDto>>   // ← nuevo

    @GET("v1/lines/{lineId}/shape")                 // ← NUEVO
    suspend fun getLineShape(@Path("lineId") lineId: String): Response<List<LatLngDto>>

}
