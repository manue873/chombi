package com.negocio.chombi.core.usecase

import com.negocio.chombi.core.ports.`in`.StartTrackingUseCase
import com.negocio.chombi.core.ports.`in`.StopTrackingUseCase
import com.negocio.chombi.core.ports.out.LocationPublisher
import com.negocio.chombi.core.ports.out.LocationSource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

class TrackingService(
    private val source: LocationSource,
    private val publisher: LocationPublisher,
    private val minSendIntervalMs: Long = 10_000 // intervalo para no spamear
) : StartTrackingUseCase, StopTrackingUseCase {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    override suspend fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            var lastSent = 0L
            source.stream().collect { s ->
                val now = System.currentTimeMillis()
                if (now - lastSent >= minSendIntervalMs) {
                    runCatching { publisher.publish(s) }
                    lastSent = now
                }
            }
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
    }
}
