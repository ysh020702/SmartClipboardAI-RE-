package com.samsung.smartclipboard.presentation.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.samsung.smartclipboard.presentation.SmartClipboardTheme
import com.samsung.smartclipboard.presentation.main.permission.MediaPermissionHelper
import com.samsung.smartclipboard.presentation.main.permission.PermissionScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var hasMediaPermission by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasMediaPermission = MediaPermissionHelper.hasImageReadPermission(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hasMediaPermission = MediaPermissionHelper.hasImageReadPermission(this)

        setContent {
            SmartClipboardTheme {
                if (hasMediaPermission) {
                    MainScreen()
                } else {
                    PermissionScreen(
                        onRequestPermission = {
                            requestMediaPermission()
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hasMediaPermission = MediaPermissionHelper.hasImageReadPermission(this)
    }

    private fun requestMediaPermission() {
        permissionLauncher.launch(
            MediaPermissionHelper.requiredMediaPermissions()
        )
    }
}