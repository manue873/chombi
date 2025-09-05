// :app/src/main/java/com/negocio/chombi/MainActivity.kt
package com.negocio.chombi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.navigation.compose.*
import com.negocio.chombi.ui.Routes
import com.negocio.chombi.ui.screens.SplashScreen
import com.negocio.chombi.ui.screens.HomeScreen
import com.negocio.chombi.ui.screens.LoginScreen
import com.negocio.chombi.ui.passenger.PassengerMapScreen
import com.negocio.chombi.ui.driver.DriverScreen
import com.negocio.chombi.ui.theme.ChombiTheme

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ServiceLocator.init(application)

        setContent {
            ChombiTheme {
                val nav = rememberNavController()
                Scaffold(
                    topBar = { CenterAlignedTopAppBar(title = { Text("Chombi") }) }
                ) { pad ->
                    NavHost(
                        navController = nav,
                        startDestination = Routes.Splash,
                        modifier = Modifier.padding(pad)
                    ) {
                        composable(Routes.Splash) { SplashScreen(nav) }
                        composable(Routes.Home) { HomeScreen(nav) }
                        composable(Routes.Login) { LoginScreen(nav) }
                        composable(Routes.Passenger) { PassengerMapScreen() }
                        composable(Routes.Driver) { DriverScreen() }
                    }
                }
            }
        }
    }
}
