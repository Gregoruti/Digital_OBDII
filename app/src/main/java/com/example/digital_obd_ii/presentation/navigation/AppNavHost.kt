package com.example.digital_obd_ii.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.digital_obd_ii.presentation.connection.ui.DeviceListScreen
import com.example.digital_obd_ii.presentation.dashboard.ui.DashboardScreen
import com.example.digital_obd_ii.presentation.debug.ui.ObdTerminalScreen
import com.example.digital_obd_ii.presentation.profile.ui.PerformanceSettingsScreen
import com.example.digital_obd_ii.presentation.profile.ui.VehicleProfileScreen
import com.example.digital_obd_ii.presentation.profile.ui.VisualSettingsScreen

sealed class Screen(val route: String) {
    object DeviceList : Screen("device_list")
    object Profile : Screen("profile")
    object VisualSettings : Screen("visual_settings")
    object Terminal : Screen("terminal")
    object Dashboard : Screen("dashboard")
    object Performance : Screen("performance")
}

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Dashboard.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.DeviceList.route) {
            DeviceListScreen(onDeviceConnected = {
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.DeviceList.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Profile.route) {
            VehicleProfileScreen(
                onBack = { navController.popBackStack() },
                onTerminalClick = { navController.navigate(Screen.Terminal.route) },
                onDashboardClick = { navController.navigate(Screen.Dashboard.route) },
                onVisualClick = { navController.navigate(Screen.VisualSettings.route) },
                onBluetoothClick = { navController.navigate(Screen.DeviceList.route) },
                onPerformanceClick = { navController.navigate(Screen.Performance.route) }
            )
        }
        composable(Screen.VisualSettings.route) {
            VisualSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Terminal.route) {
            ObdTerminalScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Performance.route) {
            PerformanceSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Dashboard.route) {
            DashboardScreen(onSettingsClick = { navController.navigate(Screen.Profile.route) })
        }
    }
}
