package com.bilal.humanactivityrecognition

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.bilal.humanactivityrecognition.service.ActivityRecognitionService
import com.bilal.humanactivityrecognition.ui.navigation.AppNavigation
import com.bilal.humanactivityrecognition.ui.theme.HARTheme
import com.bilal.humanactivityrecognition.utils.PermissionUtils

class MainActivity : ComponentActivity() {

    // Recognition works without the notification permission; it only hides the status notification.
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { startRecognition() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HARTheme {
                AppNavigation(onStartRecognition = ::requestPermissionsAndStart)
            }
        }
    }

    private fun requestPermissionsAndStart() {
        val permissions = PermissionUtils.requiredPermissions()
        if (permissions.isEmpty() || PermissionUtils.hasNotificationPermission(this)) {
            startRecognition()
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    private fun startRecognition() = ActivityRecognitionService.start(this)
}
