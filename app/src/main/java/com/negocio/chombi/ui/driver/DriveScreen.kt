package com.negocio.chombi.ui.driver

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.negocio.chombi.tracking.LocationForegroundService

@Composable
fun DriverScreen() {
    val ctx = androidx.compose.ui.platform.LocalContext.current

    // Cambia a false si quieres permitir iniciar sin background y pedirlo luego.
    val requireBgForStart = true

    // ---- Estado permisos/servicio
    var fineGranted by remember { mutableStateOf(false) }
    var coarseGranted by remember { mutableStateOf(false) }
    var bgGranted by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var locationEnabled by remember { mutableStateOf(true) }
    var trackingActive by remember { mutableStateOf(TrackingPrefs.isActive(ctx)) }
    var status by remember { mutableStateOf("Listo") }

    // API 29: pedir ACCESS_BACKGROUND_LOCATION en otro frame
    var pendingBgRequest29 by remember { mutableStateOf(false) }

    // ---- Snackbar (popup auto-dismiss)
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(status) {
        if (status.isNotBlank() && status != "Listo") {
            snackbarHostState.showSnackbar(
                message = status,
                withDismissAction = true,
                duration = SnackbarDuration.Short
            )
        }
    }

    // ---- Launchers
    val foregroundPermsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        fineGranted = granted[Manifest.permission.ACCESS_FINE_LOCATION] == true
        coarseGranted = granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        status = if (fineGranted || coarseGranted)
            "Ubicación (primer plano) otorgada" else "Ubicación (primer plano) denegada"

        if (Build.VERSION.SDK_INT == 29 && !bgGranted && (fineGranted || coarseGranted)) {
            pendingBgRequest29 = true
        }
    }

    val backgroundPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        bgGranted = granted
        status = if (bgGranted) "Ubicación en segundo plano otorgada" else "Ubicación en segundo plano denegada"
    }

    LaunchedEffect(pendingBgRequest29) {
        if (pendingBgRequest29) {
            pendingBgRequest29 = false
            backgroundPermLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    val notificationsPermLauncher =
        if (Build.VERSION.SDK_INT >= 33) {
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                notificationsEnabled = granted || areNotificationsEnabled(ctx)
                status = if (notificationsEnabled) "Notificaciones habilitadas" else "Notificaciones denegadas"
            }
        } else null

    // ---- Helpers estado
    fun refreshState() {
        fineGranted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        coarseGranted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        bgGranted = if (Build.VERSION.SDK_INT >= 29)
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        else true
        notificationsEnabled = areNotificationsEnabled(ctx)
        locationEnabled = isLocationEnabled(ctx)
        trackingActive = TrackingPrefs.isActive(ctx)
    }
    LaunchedEffect(Unit) { refreshState() }

    // ---- Solicitudes / Ajustes
    fun requestForegroundPerms() {
        foregroundPermsLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }

    fun requestBackgroundPerm() {
        when {
            Build.VERSION.SDK_INT == 29 -> pendingBgRequest29 = true
            Build.VERSION.SDK_INT >= 30 -> {
                openAppPermissionSettings(ctx)
                status = "Abre Permisos > Ubicación y elige 'Permitir siempre'"
            }
        }
    }

    fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33) {
            notificationsPermLauncher?.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            openAppNotificationSettings(ctx)
        }
    }

    fun fixMissingOnce() {
        refreshState()
        when {
            !notificationsEnabled -> requestNotifications()
            !(fineGranted || coarseGranted) -> requestForegroundPerms()
            !bgGranted && Build.VERSION.SDK_INT >= 29 -> requestBackgroundPerm()
            !locationEnabled -> openLocationSettings(ctx)
            else -> status = "Todo OK ✅"
        }
    }

    // ---- Iniciar/Detener
    fun startTracking(context: Context) {
        refreshState()
        if (!notificationsEnabled) { requestNotifications(); return }
        if (!(fineGranted || coarseGranted)) { requestForegroundPerms(); return }
        if (requireBgForStart && Build.VERSION.SDK_INT >= 29 && !bgGranted) { requestBackgroundPerm(); return }
        if (!locationEnabled) { openLocationSettings(context); return }

        LocationForegroundService.start(context)
        TrackingPrefs.setActive(context, true)
        trackingActive = true
        status = "Tracking iniciado"
    }

    fun stopTracking(context: Context) {
        LocationForegroundService.stop(context)
        TrackingPrefs.setActive(context, false)
        trackingActive = false
        status = "Tracking detenido"
    }

    // ---- UI con Scaffold para el Snackbar
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text("Conductor", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))

            // Banner de estado (verde si activo)
            TrackingStatusBanner(
                active = trackingActive,
                text = if (trackingActive) "Tracking iniciado" else "Tracking detenido"
            )

            Spacer(Modifier.height(16.dp))

            // Botonera: 3 botones iguales que ocupan todo el ancho
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { fixMissingOnce() },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text("Verificar / Arreglar", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Button(
                    onClick = { startTracking(ctx) },
                    enabled = !trackingActive && (!requireBgForStart || bgGranted || Build.VERSION.SDK_INT < 29),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text("Iniciar tracking", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                OutlinedButton(
                    onClick = { stopTracking(ctx) },
                    enabled = trackingActive,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text("Detener", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            // << Quitamos el Text(status); ahora el mensaje sale como Snackbar >>
        }
    }
}

/* ---------- Banner de estado ---------- */
@Composable
private fun TrackingStatusBanner(active: Boolean, text: String) {
    val bg = if (active) androidx.compose.ui.graphics.Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (active) androidx.compose.ui.graphics.Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurfaceVariant
    val border = if (active) androidx.compose.ui.graphics.Color(0xFF81C784) else MaterialTheme.colorScheme.outlineVariant

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = bg,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, border)
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            Text(if (active) "🟢" else "⚪", modifier = Modifier.padding(end = 8.dp))
            Text(text, color = fg, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/* ---------- Utils ---------- */
private fun areNotificationsEnabled(context: Context) =
    NotificationManagerCompat.from(context).areNotificationsEnabled()

private fun isLocationEnabled(context: Context): Boolean =
    runCatching {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lm.isLocationEnabled
        } else {
            @Suppress("DEPRECATION")
            (lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
        }
    }.getOrDefault(false)




private fun openLocationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

private fun openAppNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

private fun openAppPermissionSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        .setData(Uri.parse("package:${context.packageName}"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

/* ---------- Persistencia simple ---------- */
private object TrackingPrefs {
    private const val FILE = "chombi_prefs"
    private const val KEY = "tracking_active"
    fun isActive(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY, false)
    fun setActive(context: Context, v: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY, v).apply()
    }
}
