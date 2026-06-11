package com.khangkietson.smarthome

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.khangkietson.smarthome.ui.theme.SmartHomeTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Toast.makeText(this, "Đã cấp quyền camera", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Ứng dụng cần quyền camera để dùng tính năng cử chỉ", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val app = application as SmartHomeApp
        
        checkCameraPermission()

        setContent {
            SmartHomeTheme {
                SmartHomeAppContent(
                    deviceRepository = app.deviceRepository,
                    sceneRepository = app.sceneRepository,
                    mqttService = app.mqttService
                )
            }
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
}
