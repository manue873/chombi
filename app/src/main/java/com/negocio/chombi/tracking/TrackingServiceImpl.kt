// adapters-location/src/main/java/com/negocio/chombi/tracking/TrackingServiceImpl.kt
package com.negocio.chombi.tracking

import com.negocio.chombi.core.domain.*
import com.negocio.chombi.core.ports.`in`.StartTrackingUseCase
import com.negocio.chombi.core.ports.`in`.StopTrackingUseCase
import com.negocio.chombi.core.ports.out.LocationPublisher
import com.negocio.chombi.core.ports.out.LocationSource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

class TrackingServiceImpl(
    private val source: LocationSource,
    private val publisher: LocationPublisher,
    private val driverId: DriverId,
    private val lineId: LineId,
    private val busId: BusId,
    private val isOnline: () -> Boolean
) : StartTrackingUseCase, StopTrackingUseCase {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var trackingJob: Job? = null

    // La interfaz pide suspend:
    override suspend fun start() {
        if (trackingJob?.isActive == true) return
        trackingJob = scope.launch {
            source.stream().collectLatest { sample ->
                val enriched = sample.copy(
                    driverId = driverId,
                    lineId = lineId,
                    busId = busId
                )
                publishWithRetry(enriched)
            }
        }
    }

    // stop() NO es suspend:
    override fun stop() {
        trackingJob?.cancel()
        trackingJob = null
    }

    private suspend fun publishWithRetry(sample: LocationSample) {
        var attempt = 0
        var backoff = 1L
        val maxAttempts = 5

        while (true) {
            if (!isOnline()) {
                delay(2.seconds)
                continue
            }
            try {
                publisher.publish(sample)
                return
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: IOException) {
                // fallo de red: reintentar
            } catch (e: Exception) {
                // otros errores: no reintentar
                return
            }
            attempt++
            if (attempt >= maxAttempts) return
            delay(backoff.seconds)
            backoff = (backoff * 2).coerceAtMost(30)
        }
    }
}
