package com.negocio.chombi.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.negocio.chombi.ServiceLocator
import com.negocio.chombi.core.auth.Role
import com.negocio.chombi.ui.Routes
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(nav: NavHostController) {
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Contraseña") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())

        Button(
            onClick = {
                scope.launch {
                    loading = true
                    error = null
                    runCatching { ServiceLocator.auth.signIn(email, pass) }
                        .onSuccess { profile ->
                            if (profile.role == Role.Driver) {
                                nav.navigate(Routes.Driver) { popUpTo(Routes.Home) { inclusive = false } }
                            } else {
                                nav.navigate(Routes.Home)
                            }
                        }
                        .onFailure { error = it.message ?: "Error de inicio de sesión" }
                    loading = false
                }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (loading) "Entrando..." else "Entrar") }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
