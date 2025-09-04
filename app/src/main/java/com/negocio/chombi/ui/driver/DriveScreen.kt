package com.negocio.chombi.ui.driver

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.negocio.chombi.ServiceLocator
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@Composable
fun DriverScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("Listo") }

    fun hasPerms(): Boolean {
        val fine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    // ✅ Registrado de forma segura para Compose
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        status = if (granted.values.any { it }) "Permisos otorgados" else "Permisos denegados"
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Modo Conductor", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                launcher.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ))
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Conceder permisos") }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                if (!hasPerms()) {
                    status = "Otorga permisos primero"
                } else {
                    scope.launch {
                        ServiceLocator.startUC.start()
                        status = "Tracking ON"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Iniciar tracking") }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                ServiceLocator.stopUC.stop()
                status = "Tracking OFF"
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Detener tracking") }

        Spacer(Modifier.height(12.dp))
        Text(status)
    }
}
