package com.negocio.chombi.ui.passenger

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.app.ActivityCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.compose.runtime.Composable

@Composable
fun PassengerMapScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val mapView = rememberMapViewWithLifecycle()

    // --- Estado: permiso
    var hasLocationPermission by remember { mutableStateOf(false) }

    // --- Launcher para solicitar permisos
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasLocationPermission =
            (result[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                    (result[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
    }

    // --- Chequeo inicial del permiso
    LaunchedEffect(Unit) {
        val fine = ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        hasLocationPermission = fine || coarse
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = {
            MapsInitializer.initialize(context)
            mapView.apply {
                getMapAsync(ReadyCallback(hasLocationPermission))
            }
        },
        update = {
            // Si el permiso cambia en caliente, reconfiguramos
            it.getMapAsync(ReadyCallback(hasLocationPermission))
        }
    )
}

private class ReadyCallback(
    private val hasLocationPermission: Boolean
) : OnMapReadyCallback {

    @SuppressLint("MissingPermission") // protegemos con try/catch + flag de permiso
    override fun onMapReady(map: GoogleMap) {
        // Controles UI
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = true // <-- BOTÓN "MI UBICACIÓN"

        // Cámara a Piura
        val piura = LatLng(-5.1945, -80.6327)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(piura, 13f))

        // Marcador ejemplo
        map.addMarker(MarkerOptions().position(piura).title("Bus 101"))

        // Capa de ubicación: SOLO si hay permiso. Protegido por try/catch.
        try {
            map.isMyLocationEnabled = hasLocationPermission
        } catch (_: SecurityException) {
            map.isMyLocationEnabled = false
        }
    }
}

/** Maneja el ciclo de vida de MapView dentro de Compose */
@Composable
private fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val observer = object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) { mapView.onCreate(null) }
            override fun onStart(owner: LifecycleOwner) { mapView.onStart() }
            override fun onResume(owner: LifecycleOwner) { mapView.onResume() }
            override fun onPause(owner: LifecycleOwner) { mapView.onPause() }
            override fun onStop(owner: LifecycleOwner) { mapView.onStop() }
            override fun onDestroy(owner: LifecycleOwner) { mapView.onDestroy() }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return mapView
}
