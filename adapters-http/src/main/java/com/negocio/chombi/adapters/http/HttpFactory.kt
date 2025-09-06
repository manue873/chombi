// adapters-http/src/main/java/com/negocio/chombi/adapters/http/HttpFactory.kt
package com.negocio.chombi.adapters.http

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object HttpFactory {

    /**
     * @param baseUrl Debe apuntar a la raíz de tu API. Si no termina en "/", lo agregamos.
     * @param bearerToken Opcional: si tu backend requiere Authorization: Bearer <token>
     */
    fun api(baseUrl: String, bearerToken: String? = null): ApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // verás JSON y códigos HTTP en Logcat
        }

        val ok = OkHttpClient.Builder()
            .addInterceptor(logging)
            .apply {
                if (!bearerToken.isNullOrBlank()) {
                    addInterceptor { chain ->
                        val req = chain.request().newBuilder()
                            .addHeader("Authorization", "Bearer $bearerToken")
                            .build()
                        chain.proceed(req)
                    }
                }
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        return Retrofit.Builder()
            .baseUrl(normalizedBaseUrl)
            .client(ok)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
