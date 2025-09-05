package com.negocio.chombi.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.negocio.chombi.ServiceLocator
import com.negocio.chombi.core.auth.Role
import com.negocio.chombi.ui.Routes
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(nav: NavHostController) {
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = { nav.navigate(Routes.Passenger) }, modifier = Modifier.fillMaxWidth()) {
            Text("Entrar como Pasajero")
        }
        Button(
            onClick = {
                scope.launch {
                    val user = ServiceLocator.auth.currentUser()
                    when {
                        user == null -> nav.navigate(Routes.Login)
                        user.role != Role.Driver -> {
                            message = "Tu cuenta no tiene rol de Conductor."
                        }
                        else -> nav.navigate(Routes.Driver)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Entrar como Conductor") }

        message?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}
