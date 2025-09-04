package com.negocio.chombi.ui.passenger

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.maps.android.compose.*
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.delay
import com.negocio.chombi.adapters.http.HttpFactory
import com.negocio.chombi.adapters.http.ApiService

data class BusMarker(val driverId: String, val lat: Double, val lng: Double, val timestamp: Long)

@Composable
fun PassengerMapScreen(
    baseUrl: String = "http://10.0.2.2:8000/",
    defaultLine: String = "linea_demo",
    refreshMs: Long = 10_000
) {
    val api: ApiService = remember(baseUrl) { HttpFactory.api(baseUrl) }
    var line by remember { mutableStateOf(defaultLine) }
    var buses by remember { mutableStateOf(emptyList<BusMarker>()) }

    val camera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(-5.19449, -80.63282), 13f)
    }

    LaunchedEffect(line) {
        while (true) {
            runCatching { api.latestByLine(line) }.onSuccess { list ->
                buses = list.map { BusMarker(it.driverId, it.lat, it.lng, it.timestamp) }
                if (buses.isNotEmpty()) {
                    camera.animate(CameraUpdateFactory.newLatLng(LatLng(buses[0].lat, buses[0].lng)), 500)
                }
            }
            delay(refreshMs)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = line, onValueChange = { line = it }, label = { Text("Línea") }, modifier = Modifier.weight(1f))
            Button(onClick = { /* refresca inmediato */ buses = emptyList(); }) { Text("Limpiar") }
        }
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            properties = MapProperties(
                isMyLocationEnabled = true
            ),
            cameraPositionState = camera,
            uiSettings = MapUiSettings(zoomControlsEnabled = true, compassEnabled = true)
        ) {
            buses.forEach { b ->
                Marker(
                    state = MarkerState(LatLng(b.lat, b.lng)),
                    title = "Bus ${b.driverId}",
                    snippet = "t=${b.timestamp}"
                )
            }
        }
    }
}
