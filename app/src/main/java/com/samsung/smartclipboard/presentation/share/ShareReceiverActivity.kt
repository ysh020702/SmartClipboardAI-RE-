package com.samsung.smartclipboard.presentation.share

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.samsung.smartclipboard.presentation.SmartClipboardTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class ShareReceiverActivity : ComponentActivity() {
    /*
    * "공유" 버튼을 눌렀을 때 SmartClipboard AI로 콘텐츠를 전달받는
    * __진입점(Entry Point)__ 역할
    * */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SmartClipboardTheme {
                val viewModel: ShareReceiverViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                // Process the share intent once
                LaunchedEffect(Unit) {
                    viewModel.processShareIntent(intent)
                }

                // Auto-finish after a short delay once done
                if (uiState.shouldFinish) {
                    LaunchedEffect(Unit) {
                        delay(800L)
                        if (!isFinishing && !isDestroyed) {
                            finish()
                        }
                    }
                }

                ShareReceiverScreen(uiState = uiState)
            }
        }
    }
}