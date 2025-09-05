package com.negocio.chombi.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.negocio.chombi.ServiceLocator
import com.negocio.chombi.ui.Routes
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(nav: NavHostController) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        scope.launch {
            // Chequea sesión rápidamente
            ServiceLocator.auth.currentUser()
            nav.navigate(Routes.Home) {
                popUpTo(Routes.Splash) { inclusive = true }
            }
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
