package com.khangkietson.smarthome

import android.app.Application
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.khangkietson.smarthome.data.mqtt.MqttService
import com.khangkietson.smarthome.data.repository.DeviceRepository
import com.khangkietson.smarthome.data.repository.SceneRepository
import com.khangkietson.smarthome.navigation.NavGraph
import com.khangkietson.smarthome.navigation.Screen
import com.khangkietson.smarthome.ui.home.HomeViewModel
import com.khangkietson.smarthome.ui.scene.SceneViewModel
import com.khangkietson.smarthome.ui.gesture.GestureViewModel

class SmartHomeApp : Application() {
    lateinit var mqttService: MqttService
    lateinit var deviceRepository: DeviceRepository
    lateinit var sceneRepository: SceneRepository

    override fun onCreate() {
        super.onCreate()
        // Cấu hình JVM ưu tiên sử dụng ngăn xếp IPv4 cho toàn bộ vòng đời ứng dụng
        System.setProperty("java.net.preferIPv4Stack", "true")
        System.setProperty("java.net.preferIPv6Addresses", "false")
        
        mqttService = MqttService()
        deviceRepository = DeviceRepository(this, mqttService)
        sceneRepository = SceneRepository(deviceRepository)
    }
}

@Composable
fun SmartHomeAppContent(
    deviceRepository: DeviceRepository,
    sceneRepository: SceneRepository,
    mqttService: MqttService
) {
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.provideFactory(deviceRepository))
    val sceneViewModel: SceneViewModel = viewModel(factory = SceneViewModel.provideFactory(sceneRepository))
    val gestureViewModel: GestureViewModel = viewModel(factory = GestureViewModel.provideFactory(deviceRepository))

    val items = listOf(
        Screen.Home,
        Screen.Scene,
        Screen.Gesture
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Lắng nghe vòng đời của Activity để tự động reconnect khi mở lại điện thoại (resume)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val state = mqttService.connectionState.value
                val activeRoute = navController.currentBackStackEntry?.destination?.route
                val isNotOnSplash = activeRoute != null && activeRoute != Screen.Splash.route
                
                // Chỉ điều hướng về Splash nếu app đang ở màn hình khác và bị mất kết nối.
                // Tránh điều hướng khi đang khởi chạy màn hình Splash đầu tiên để tránh lỗi crash do NavController chưa sẵn sàng.
                if (isNotOnSplash && (state == MqttService.ConnectionStatus.DISCONNECTED || state == MqttService.ConnectionStatus.FAILED)) {
                    navController.navigate(Screen.Splash.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        bottomBar = {
            if (currentRoute != Screen.Splash.route) {
                NavigationBar {
                    items.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            selected = isSelected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavGraph(
            navController = navController,
            homeViewModel = homeViewModel,
            sceneViewModel = sceneViewModel,
            gestureViewModel = gestureViewModel,
            mqttService = mqttService,
            scaffoldPadding = innerPadding
        )
    }
}
