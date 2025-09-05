package com.negocio.chombi.ui.driver

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.negocio.chombi.ServiceLocator
import com.negocio.chombi.tracking.AppPrefs
import com.negocio.chombi.tracking.DriverContext
import com.negocio.chombi.tracking.TrackingService

@Composable
fun DriverScreen() {

    val ctx = LocalContext.current
    var status by remember { mutableStateOf("Listo") }
    var running by remember { mutableStateOf(false) }
    var driverCtx by remember { mutableStateOf<DriverContext?>(null) }

    LaunchedEffect(Unit) {
        running = AppPrefs(ctx).isDriverActive()
        status = if (running) "Tracking ON (Foreground Service)" else "Listo"
        // cargar asignación (driverId + busId)
        ServiceLocator.refreshDriverContext()
        driverCtx = ServiceLocator.getDriverContext()
    }
    fun hasFineOrCoarse(): Boolean {
        val fine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    fun hasBgLocation(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true
        val bg = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        return bg == PackageManager.PERMISSION_GRANTED
    }

    fun hasPostNotifs(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        val noti = ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
        return noti == PackageManager.PERMISSION_GRANTED
    }

    // Lanzadores de permisos
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        status = if (granted.values.any { it }) "Permisos ubicación OK" else "Permisos ubicación denegados"
    }

    val bgLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        status = if (granted) "Background location OK" else "Background location denegado"
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        status = if (granted) "Notificaciones OK" else "Notificaciones denegadas"
    }

    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Modo Conductor", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))

        // ✅ Info de asignación
        if (driverCtx != null) {
            Text("Conductor: ${driverCtx!!.driverId.value}", fontWeight = FontWeight.SemiBold)
            Text("Bus asignado: ${driverCtx!!.busId.value}", fontWeight = FontWeight.SemiBold)
        } else {
            Text(
                "Tu cuenta no tiene bus asignado.\nSolicita a un administrador que te asigne uno.",
                color = MaterialTheme.colorScheme.error
            )
        }

        // 2) (Opcional) Background location para Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    if (!hasBgLocation()) {
                        bgLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    } else {
                        status = "Background location ya otorgado"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Permitir ubicación en 2º plano") }
        }

        Spacer(Modifier.height(8.dp))

        // 3) Iniciar tracking via Foreground Service
        Button(
            enabled = !running && driverCtx != null,
            onClick = {
                when {
                    !hasFineOrCoarse() -> status = "Otorga ubicación primero"
                    !hasPostNotifs() -> status = "Otorga notificaciones primero"
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasBgLocation() ->
                        status = "Otorga background location (recomendado) antes de iniciar"
                    driverCtx == null ->
                        status = "No puedes iniciar sin bus asignado"
                    else -> {
                        TrackingService.start(ctx)
                        running = true
                        status = "Tracking ON (Foreground Service)"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Iniciar tracking") }

        Spacer(Modifier.height(8.dp))

        Button(
            enabled = running,
            onClick = {
                TrackingService.stop(ctx)
                running = false
                status = "Tracking OFF"
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Detener tracking") }

        Spacer(Modifier.height(12.dp))
        Text(status)
    }
}
