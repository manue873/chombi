// app/src/main/java/com/negocio/chombi/ui/passenger/PassengerMapScreen.kt
package com.negocio.chombi.ui.passenger

import android.Manifest
import com.negocio.chombi.R
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.*
import com.negocio.chombi.ServiceLocator
import com.negocio.chombi.adapters.http.LineDto
import com.negocio.chombi.adapters.http.LocationReadDto
import com.negocio.chombi.prefs.UserPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PassengerMapScreen(
    modifier: Modifier = Modifier,
    fallbackLineId: String = "L45",
    pollMs: Long = 5_000L
) {
    val context = LocalContext.current
    val mapView = rememberMapViewWithLifecycle()
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    var mapLoaded by remember { mutableStateOf(false) }

    // Toggles UI
    var autoFollow by remember { mutableStateOf(true) }
    var showTrail by remember { mutableStateOf(true) }

    // Estado de seguimiento/recentrado
    var didAutoRecenter by remember { mutableStateOf(false) }
    var lastFollowAt by remember { mutableStateOf(0L) }

    // Marcadores y líneas dinámicas por bus
    val markers = remember { mutableStateMapOf<String, Marker>() }
    val trails = remember { mutableStateMapOf<String, MutableList<LatLng>>() }
    val trailPolylines = remember { mutableStateMapOf<String, Polyline>() }
    var lineShapePolyline by remember { mutableStateOf<Polyline?>(null) }
    val MAX_TRAIL_POINTS = 60

    // Permisos
    var hasLocationPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasLocationPermission =
            (result[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                    (result[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
        googleMap?.let { m -> try { m.isMyLocationEnabled = hasLocationPermission } catch (_: SecurityException) {} }
    }
    LaunchedEffect(Unit) {
        val fine = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        hasLocationPermission = fine || coarse
        if (!hasLocationPermission) {
            permissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    // Líneas desde backend + persistencia de selección
    var lines by remember { mutableStateOf<List<LineDto>>(emptyList()) }
    var selectedLine by remember { mutableStateOf(fallbackLineId) }

    LaunchedEffect(Unit) {
        val repo = ServiceLocator.linesRepository
        val remote = withContext(Dispatchers.IO) { repo.getLines() }
        lines = remote.ifEmpty { listOf(LineDto(fallbackLineId, "Línea $fallbackLineId")) }

        val saved = withContext(Dispatchers.IO) { UserPrefs.getLastLineId(context) }
        selectedLine = when {
            saved != null && lines.any { it.id == saved } -> saved
            else -> lines.first().id
        }
    }

    // Guardar selección y cargar shape de la línea
    LaunchedEffect(selectedLine, mapLoaded) {
        withContext(Dispatchers.IO) { UserPrefs.setLastLineId(context, selectedLine) }

        googleMap?.clear()
        markers.clear()
        trails.clear()
        trailPolylines.values.forEach { it.remove() }
        trailPolylines.clear()
        lineShapePolyline = null
        didAutoRecenter = false

        val repo = ServiceLocator.linesRepository
        val shape = withContext(Dispatchers.IO) {
            try { repo.getLineShape(selectedLine) } catch (_: Exception) { emptyList() }
        }

        if (shape.isNotEmpty() && googleMap != null && mapLoaded) {
            val pts = shape.map { LatLng(it.lat, it.lng) }
            lineShapePolyline = googleMap!!.addPolyline(
                PolylineOptions()
                    .addAll(pts)
                    .width(8f)
                    .color(android.graphics.Color.GRAY)
                    .geodesic(true)
            )
            recenterToPoints(googleMap, pts)
        }
    }

    // Tiempo de última actualización + conteo
    var lastUpdatedAt by remember { mutableStateOf<Long?>(null) }
    var busesCount by remember { mutableStateOf(0) }

    Box(modifier.fillMaxSize()) {
        // MAPA
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                MapsInitializer.initialize(context)
                mapView.apply {
                    getMapAsync { map ->
                        googleMap = map
                        configureMap(map, hasLocationPermission)
                        val piura = LatLng(-5.1945, -80.6327)
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(piura, 13f))
                        map.setOnMapLoadedCallback { mapLoaded = true }
                    }
                }
            },
            update = {
                googleMap?.let { m ->
                    try { m.isMyLocationEnabled = hasLocationPermission } catch (_: SecurityException) {}
                }
            }
        )

        // Overlay superior estilizado (envuelve si falta espacio)
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(12.dp)
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
            shadowElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.padding(10.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Selector de línea
                    var open by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = open,
                        onExpandedChange = { open = it },
                        modifier = Modifier
                            .widthIn(min = 200.dp)
                            .weight(1f, fill = false)
                    ) {
                        TextField(
                            value = lines.firstOrNull { it.id == selectedLine }?.name ?: selectedLine,
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            label = { Text("Línea") },
                            leadingIcon = { Icon(Icons.Rounded.DirectionsBus, contentDescription = null) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) }
                        )
                        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                            lines.forEach { line ->
                                DropdownMenuItem(
                                    text = { Text(line.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    onClick = { selectedLine = line.id; open = false }
                                )
                            }
                        }
                    }

                    // Chips de control
                    FilterChip(
                        selected = autoFollow,
                        onClick = { autoFollow = !autoFollow },
                        label = { Text(if (autoFollow) "Seguir: ON" else "Seguir: OFF") },
                        leadingIcon = { Icon(Icons.Rounded.CenterFocusStrong, contentDescription = null) }
                    )
                    FilterChip(
                        selected = showTrail,
                        onClick = { showTrail = !showTrail },
                        label = { Text(if (showTrail) "Trail: ON" else "Trail: OFF") },
                        leadingIcon = { Icon(Icons.Rounded.Route, contentDescription = null) }
                    )

                    // Recentrar
                    FilledTonalButton(onClick = { recenterOnCluster(googleMap, markers.values.toList()) }) {
                        Text("Recentrar")
                    }
                }
            }
        }

        // ——— ZOOM (+/–) ———
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 96.dp), // separado del FAB "Mi ubicación"
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.End
        ) {
            SmallFloatingActionButton(
                onClick = { googleMap?.animateCamera(CameraUpdateFactory.zoomIn()) }
            ) { Icon(Icons.Rounded.Add, contentDescription = "Acercar") }

            SmallFloatingActionButton(
                onClick = { googleMap?.animateCamera(CameraUpdateFactory.zoomOut()) }
            ) { Icon(Icons.Rounded.Remove, contentDescription = "Alejar") }
        }

        // FAB de mi ubicación (por si desactivas el botón nativo)
        ExtendedFloatingActionButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            onClick = {
                googleMap?.let { m ->
                    try {
                        m.isMyLocationEnabled = hasLocationPermission
                        // Aquí podrías centrar a la última ubicación conocida si la expones.
                    } catch (_: SecurityException) { }
                }
            },
            icon = { Icon(Icons.Rounded.MyLocation, contentDescription = null) },
            text = { Text("Mi ubicación") }
        )

        // Overlay inferior: estado en card
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .wrapContentWidth(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 4.dp,
            shadowElevation = 4.dp
        ) {
            Row(
                Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Buses: $busesCount • Refresco: ${pollMs/1000}s" +
                            (lastUpdatedAt?.let { " • Última: ${max(0L,(System.currentTimeMillis()-it)/1000)}s" } ?: ""),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Polling y actualización de buses + trail (sin cambios)
    LaunchedEffect(selectedLine, googleMap, mapLoaded, showTrail, autoFollow) {
        if (googleMap == null || !mapLoaded) return@LaunchedEffect
        val repo = ServiceLocator.linesRepository
        val map = googleMap!!

        val busIcon = bitmapDescriptorFromVector(context, R.drawable.ic_bus_filled, sizeDp = 36f)
        val staleThresholdMs = 15_000L // más estricto para UX

        while (true) {
            val buses: List<LocationReadDto> = withContext(Dispatchers.IO) {
                try { repo.latestByLine(selectedLine) } catch (_: Exception) { emptyList() }
            }.filter { System.currentTimeMillis() - it.timestamp <= staleThresholdMs }

            busesCount = buses.size

            // Remover buses que ya no están
            val liveIds = buses.map { it.busId }.toSet()
            val toRemove = markers.keys - liveIds
            toRemove.forEach { id ->
                markers.remove(id)?.remove()
                trailPolylines.remove(id)?.remove()
                trails.remove(id)
            }

            // Agregar/actualizar buses y su trail
            buses.forEach { b ->
                val pos = LatLng(b.lat, b.lng)
                val mk = markers[b.busId]
                if (mk == null) {
                    val added = map.addMarker(
                        MarkerOptions()
                            .position(pos)
                            .title("Bus ${b.busId}")
                            .icon(busIcon)
                            .flat(true)
                            .anchor(0.5f, 0.5f)
                            .rotation(b.bearing ?: 0f)
                    )
                    if (added != null) markers[b.busId] = added
                } else {
                    val animMs = max(300L, pollMs - 100L)
                    // Calcular bearing si no viene
                    val prev = trails[b.busId]?.lastOrNull()
                    mk.rotation = b.bearing ?: (prev?.let { bearingBetween(it, pos) } ?: mk.rotation)
                    animateMarkerTo(mk, pos, animMs)
                    mk.isFlat = true
                }

                // Trail (si está activo) con limpieza por saltos grandes
                if (showTrail) {
                    val trail = trails.getOrPut(b.busId) { mutableListOf() }
                    val last = trail.lastOrNull()
                    if (last != null && haversineKm(last, pos) > 2.0) {
                        trail.clear()
                        trailPolylines[b.busId]?.points = emptyList()
                    }
                    trail.add(pos)
                    if (trail.size > MAX_TRAIL_POINTS) trail.removeAt(0)

                    val poly = trailPolylines[b.busId]
                    if (poly == null) {
                        val p = map.addPolyline(
                            PolylineOptions()
                                .addAll(trail)
                                .width(6f)
                                .color(android.graphics.Color.parseColor("#007AFF"))
                                .geodesic(true)
                        )
                        trailPolylines[b.busId] = p
                    } else {
                        poly.points = trail
                    }
                } else {
                    trails[b.busId]?.clear()
                    trailPolylines.remove(b.busId)?.remove()
                }
            }

            // Recentrado automático (throttle 3s)
            if (autoFollow && markers.isNotEmpty()) {
                val now = System.currentTimeMillis()
                if (!didAutoRecenter || now - lastFollowAt > 3000) {
                    recenterOnCluster(googleMap, markers.values.toList())
                    didAutoRecenter = true
                    lastFollowAt = now
                }
            }

            lastUpdatedAt = System.currentTimeMillis()
            delay(pollMs)
        }
    }
}

@SuppressLint("MissingPermission")
private fun configureMap(map: GoogleMap, hasLocationPermission: Boolean) {
    map.uiSettings.isZoomControlsEnabled = false // usamos controles propios
    map.uiSettings.isMyLocationButtonEnabled = true
    try {
        map.isMyLocationEnabled = hasLocationPermission
    } catch (_: SecurityException) {
        map.isMyLocationEnabled = false
    }
}

private fun animateMarkerTo(marker: Marker, finalPosition: LatLng, durationMs: Long) {
    val start = marker.position
    if (start == finalPosition) return
    val animator = ValueAnimator.ofFloat(0f, 1f).setDuration(durationMs)
    animator.addUpdateListener { va ->
        val t = va.animatedFraction
        val lat = start.latitude + (finalPosition.latitude - start.latitude) * t
        val lng = start.longitude + (finalPosition.longitude - start.longitude) * t
        marker.position = LatLng(lat, lng)
    }
    animator.start()
}

private fun recenterOnCluster(map: GoogleMap?, markers: List<Marker>) {
    if (map == null || markers.isEmpty()) return
    val b = LatLngBounds.Builder()
    markers.forEach { b.include(it.position) }
    map.animateCamera(CameraUpdateFactory.newLatLngBounds(b.build(), 80))
}

private fun recenterToPoints(map: GoogleMap?, points: List<LatLng>) {
    if (map == null || points.isEmpty()) return
    val b = LatLngBounds.Builder()
    points.forEach { b.include(it) }
    map.animateCamera(CameraUpdateFactory.newLatLngBounds(b.build(), 80))
}

private fun bitmapDescriptorFromVector(
    context: android.content.Context,
    @DrawableRes resId: Int,
    sizeDp: Float = 36f
): BitmapDescriptor {
    val drawable = AppCompatResources.getDrawable(context, resId)
        ?: return BitmapDescriptorFactory.defaultMarker()
    val density = context.resources.displayMetrics.density
    val w = (sizeDp * density).toInt().coerceAtLeast(1)
    val h = (sizeDp * density).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, w, h)
    drawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

private fun bearingBetween(a: LatLng, b: LatLng): Float {
    val lat1 = Math.toRadians(a.latitude)
    val lat2 = Math.toRadians(b.latitude)
    val dLon = Math.toRadians(b.longitude - a.longitude)
    val y = sin(dLon) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
    val brng = Math.toDegrees(atan2(y, x))
    return ((brng + 360) % 360).toFloat()
}

private fun haversineKm(a: LatLng, b: LatLng): Double {
    val R = 6371.0
    val dLat = Math.toRadians(b.latitude - a.latitude)
    val dLon = Math.toRadians(b.longitude - a.longitude)
    val lat1 = Math.toRadians(a.latitude)
    val lat2 = Math.toRadians(b.latitude)
    val h = sin(dLat/2).pow(2.0) + sin(dLon/2).pow(2.0) * cos(lat1) * cos(lat2)
    return 2 * R * asin(min(1.0, sqrt(h)))
}

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
