package com.example.digital_obd_ii.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.digital_obd_ii.presentation.communication.ui.CommunicationScreen
import com.example.digital_obd_ii.presentation.connection.ui.ConnectionStatusScreen
import com.example.digital_obd_ii.presentation.connection.ui.DeviceListScreen
import com.example.digital_obd_ii.presentation.dashboard.ui.DashboardScreen
import com.example.digital_obd_ii.presentation.debug.ui.ObdTerminalScreen
import com.example.digital_obd_ii.presentation.profile.ui.PerformanceSettingsScreen
import com.example.digital_obd_ii.presentation.profile.ui.VehicleProfileScreen
import com.example.digital_obd_ii.presentation.profile.ui.VisualSettingsScreen

sealed class Screen(val route: String) {
    object DeviceList : Screen("device_list")
    object ConnectionStatus : Screen("connection_status/{deviceAddress}") {
        fun createRoute(deviceAddress: String) = "connection_status/$deviceAddress"
    }
    object Profile : Screen("profile")
    object VisualSettings : Screen("visual_settings")
    object Terminal : Screen("terminal")
    object Dashboard : Screen("dashboard")
    object Performance : Screen("performance")
    object Communication : Screen("communication")
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
            DeviceListScreen(
                onDeviceSelected = { deviceAddress ->
                    navController.navigate(Screen.ConnectionStatus.createRoute(deviceAddress))
                }
            )
        }
        composable(
            route = Screen.ConnectionStatus.route,
            arguments = listOf(navArgument("deviceAddress") { type = NavType.StringType })
        ) { backStackEntry ->
            val address = backStackEntry.arguments?.getString("deviceAddress") ?: ""
            ConnectionStatusScreen(
                deviceAddress = address,
                onConnectedAndReady = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.DeviceList.route) { inclusive = true }
                    }
                },
                onBackToDeviceList = {
                    navController.navigate(Screen.DeviceList.route) {
                        popUpTo(Screen.DeviceList.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Profile.route) {
            VehicleProfileScreen(
                onBack = { navController.popBackStack() },
                onTerminalClick = { navController.navigate(Screen.Terminal.route) },
                onDashboardClick = { navController.navigate(Screen.Dashboard.route) },
                onVisualClick = { navController.navigate(Screen.VisualSettings.route) },
                onBluetoothClick = { navController.navigate(Screen.DeviceList.route) },
                onPerformanceClick = { navController.navigate(Screen.Performance.route) },
                onCommunicationClick = { navController.navigate(Screen.Communication.route) }
            )
        }
        composable(Screen.Communication.route) {
            CommunicationScreen(onBack = { navController.popBackStack() })
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
