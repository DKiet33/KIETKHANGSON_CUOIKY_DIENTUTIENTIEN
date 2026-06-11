package com.khangkietson.smarthome.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BackHand
import androidx.compose.material.icons.outlined.Home
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.khangkietson.smarthome.data.mqtt.MqttService
import com.khangkietson.smarthome.ui.gesture.GestureScreen
import com.khangkietson.smarthome.ui.gesture.GestureViewModel
import com.khangkietson.smarthome.ui.home.HomeScreen
import com.khangkietson.smarthome.ui.home.HomeViewModel
import com.khangkietson.smarthome.ui.scene.SceneScreen
import com.khangkietson.smarthome.ui.scene.SceneViewModel
import com.khangkietson.smarthome.ui.splash.SplashScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Splash : Screen("splash", "Kết nối", Icons.Outlined.Home)
    object Home : Screen("home", "Trang chủ", Icons.Outlined.Home)
    object Scene : Screen("scene", "Kịch bản", Icons.Outlined.AutoAwesome)
    object Gesture : Screen("gesture", "Cử chỉ", Icons.Outlined.BackHand)
}

@Composable
fun NavGraph(
    navController: NavHostController,
    homeViewModel: HomeViewModel,
    sceneViewModel: SceneViewModel,
    gestureViewModel: GestureViewModel,
    mqttService: MqttService,
    scaffoldPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
        modifier = modifier
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                mqttService = mqttService,
                onConnectionSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = homeViewModel,
                modifier = Modifier.padding(scaffoldPadding)
            )
        }
        composable(Screen.Scene.route) {
            SceneScreen(
                viewModel = sceneViewModel,
                modifier = Modifier.padding(scaffoldPadding)
            )
        }
        composable(Screen.Gesture.route) {
            GestureScreen(
                viewModel = gestureViewModel,
                modifier = Modifier.padding(scaffoldPadding)
            )
        }
    }
}
