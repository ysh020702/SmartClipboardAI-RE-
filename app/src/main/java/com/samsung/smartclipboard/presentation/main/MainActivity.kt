package com.samsung.smartclipboard.presentation.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.samsung.smartclipboard.data.source.local.CollectionPeriodPreferences
import com.samsung.smartclipboard.presentation.SmartClipboardTheme
import com.samsung.smartclipboard.presentation.main.permission.MediaPermissionHelper
import com.samsung.smartclipboard.presentation.main.permission.OnboardingDateScreen
import com.samsung.smartclipboard.presentation.main.permission.PermissionScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var collectionPeriodPreferences: CollectionPeriodPreferences

    private var hasMediaPermission by mutableStateOf(false)
    private var isOnboardingCompleted by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasMediaPermission = MediaPermissionHelper.hasImageReadPermission(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hasMediaPermission = MediaPermissionHelper.hasImageReadPermission(this)

        // 온보딩 완료 여부 확인
        CoroutineScope(Dispatchers.Main).launch {
            collectionPeriodPreferences.isOnboardingCompleted.collect { completed ->
                isOnboardingCompleted = completed
            }
        }

        setContent {
            SmartClipboardTheme {
                when {
                    !hasMediaPermission -> PermissionScreen(
                        onRequestPermission = {
                            requestMediaPermission()
                        }
                    )

                    !isOnboardingCompleted -> OnboardingDateScreen(
                        onComplete = { startMs, endMs ->
                            CoroutineScope(Dispatchers.IO).launch {
                                collectionPeriodPreferences.setCollectionPeriod(startMs, endMs)
                                collectionPeriodPreferences.setOnboardingCompleted()
                            }
                        }
                    )

                    else -> MainScreen()
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