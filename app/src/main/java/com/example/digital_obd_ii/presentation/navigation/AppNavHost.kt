package com.example.digital_obd_ii.presentation.navigation

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.digital_obd_ii.presentation.connection.ui.DeviceListScreen
import com.example.digital_obd_ii.presentation.profile.ui.VehicleProfileScreen
import com.example.digital_obd_ii.presentation.profile.ui.VisualSettingsScreen
import com.example.digital_obd_ii.presentation.debug.ui.ObdTerminalScreen
import com.example.digital_obd_ii.presentation.dashboard.ui.DashboardScreen

sealed class Screen(val route: String) {
    object DeviceList : Screen("device_list")
    object Profile : Screen("profile")
    object VisualSettings : Screen("visual_settings")
    object Terminal : Screen("terminal")
    object Dashboard : Screen("dashboard")
}

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.DeviceList.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.DeviceList.route) {
            DeviceListScreen(onDeviceConnected = {
                navController.navigate(Screen.Profile.route)
            })
        }
        composable(Screen.Profile.route) {
            VehicleProfileScreen(onBack = {
                navController.popBackStack()
            }, onTerminalClick = {
                navController.navigate(Screen.Terminal.route)
            }, onDashboardClick = {
                navController.navigate(Screen.Dashboard.route)
            }, onVisualClick = {
                navController.navigate(Screen.VisualSettings.route)
            })
        }
        composable(Screen.VisualSettings.route) {
            VisualSettingsScreen(onBack = {
                navController.popBackStack()
            })
        }
        composable(Screen.Terminal.route) {
            ObdTerminalScreen(onBack = {
                navController.popBackStack()
            })
        }
        composable(Screen.Dashboard.route) {
            DashboardScreen()
        }
    }
}
