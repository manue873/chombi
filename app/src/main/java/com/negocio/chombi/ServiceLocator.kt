package com.negocio.chombi

import android.app.Application
import com.negocio.chombi.adapters.http.HttpFactory
import com.negocio.chombi.adapters.http.RetrofitLocationPublisher
import com.negocio.chombi.adapters.location.FusedLocationSource
import com.negocio.chombi.core.domain.DriverId
import com.negocio.chombi.core.domain.LineId
import com.negocio.chombi.core.ports.`in`.StartTrackingUseCase
import com.negocio.chombi.core.ports.`in`.StopTrackingUseCase
import com.negocio.chombi.core.usecase.TrackingService

object ServiceLocator {
    lateinit var startUC: StartTrackingUseCase
    lateinit var stopUC: StopTrackingUseCase

    fun init(app: Application) {
        val driver = DriverId("driver_demo")
        val line = LineId("linea_demo")

        val source = FusedLocationSource(app, driver, line)
        val api = HttpFactory.api("http://10.0.2.2:8000/")
        val publisher = RetrofitLocationPublisher(api)

        val svc = TrackingService(source, publisher, minSendIntervalMs = 1_000)
        startUC = svc
        stopUC = svc
    }
}
