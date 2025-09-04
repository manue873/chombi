package com.negocio.chombi.adapters.http

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object HttpFactory {
    fun api(baseUrl: String): ApiService {
        val ok = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(ok)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
