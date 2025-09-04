package com.negocio.chombi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import com.negocio.chombi.ui.Routes
import com.negocio.chombi.ui.driver.DriverScreen
import com.negocio.chombi.ui.passenger.PassengerMapScreen
import com.negocio.chombi.ui.theme.ChombiTheme
@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ServiceLocator.init(application) // Ensambla core + adapters

        setContent {
            ChombiTheme {
                val navController = rememberNavController()
                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text("Chombi") },
                            actions = {
                                TextButton(onClick = { navController.navigate(Routes.Driver) }) {
                                    Text("Conductor")
                                }
                                TextButton(onClick = { navController.navigate(Routes.Passenger) }) {
                                    Text("Pasajero")
                                }
                            },
                            scrollBehavior = null
                        )
                    }
                ) { padding ->
                    NavHost(
                        navController = navController,
                        startDestination = Routes.Driver,
                        modifier = Modifier.padding(padding)
                    ) {
                        composable(Routes.Driver) { DriverScreen() }
                        composable(Routes.Passenger) { PassengerMapScreen() }
                    }
                }

            }
        }
    }
}
