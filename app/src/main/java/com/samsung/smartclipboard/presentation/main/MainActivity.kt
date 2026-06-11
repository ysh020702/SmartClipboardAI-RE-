package com.samsung.smartclipboard.presentation.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.samsung.smartclipboard.data.source.period.CollectionPeriodManager
import com.samsung.smartclipboard.presentation.SmartClipboardTheme
import com.samsung.smartclipboard.presentation.main.permission.CollectionPeriodSetupScreen
import com.samsung.smartclipboard.presentation.main.permission.MediaPermissionHelper
import com.samsung.smartclipboard.presentation.main.permission.PermissionScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var hasMediaPermission by mutableStateOf(false)
    private var showPeriodSetup by mutableStateOf(false)

    @Inject
    lateinit var collectionPeriodManager: CollectionPeriodManager

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasMediaPermission = MediaPermissionHelper.hasImageReadPermission(this)

        if (hasMediaPermission) {
            val prefs = getSharedPreferences("app_setup", MODE_PRIVATE)
            val hasCompletedSetup = prefs.getBoolean("period_setup_completed", false)
            showPeriodSetup = !hasCompletedSetup
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hasMediaPermission = MediaPermissionHelper.hasImageReadPermission(this)

        if (hasMediaPermission) {
            val prefs = getSharedPreferences("app_setup", MODE_PRIVATE)
            val hasCompletedSetup = prefs.getBoolean("period_setup_completed", false)
            showPeriodSetup = !hasCompletedSetup
        }

        setContent {
            SmartClipboardTheme {
                when {
                    !hasMediaPermission -> {
                        PermissionScreen(
                            onRequestPermission = {
                                requestMediaPermission()
                            }
                        )
                    }
                    showPeriodSetup -> {
                        CollectionPeriodSetupScreen(
                            onStartPeriodSelected = { timestamp ->
                                collectionPeriodManager.startDate = timestamp
                            },
                            onEndPeriodSelected = { timestamp ->
                                collectionPeriodManager.endDate = timestamp
                            },
                            onComplete = {
                                getSharedPreferences("app_setup", MODE_PRIVATE)
                                    .edit()
                                    .putBoolean("period_setup_completed", true)
                                    .apply()
                                showPeriodSetup = false
                            }
                        )
                    }
                    else -> {
                        MainScreen()
                    }
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