package com.samsung.smartclipboard.presentation.clipboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samsung.smartclipboard.ui.theme.SmartClipboardTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class ClipboardCaptureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SmartClipboardTheme {
                val viewModel: ClipboardCaptureViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                // Small delay to let the Activity gain focus, then capture
                LaunchedEffect(Unit) {
                    delay(100L)
                    viewModel.captureClipboard()
                }

                // Auto-finish after done
                if (uiState.shouldFinish) {
                    LaunchedEffect(Unit) {
                        delay(700L)
                        if (!isFinishing && !isDestroyed) {
                            finish()
                        }
                    }
                }

                ClipboardCaptureScreen(uiState = uiState)
            }
        }
    }
}