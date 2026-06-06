package com.samsung.smartclipboard.presentation.clipboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.smartclipboard.data.source.clipboard.ClipboardCaptureHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClipboardCaptureViewModel @Inject constructor(
    private val captureHandler: ClipboardCaptureHandler
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClipboardCaptureUiState())
    val uiState: StateFlow<ClipboardCaptureUiState> = _uiState

    private var processed = false

    fun captureClipboard() {
        if (processed) return
        processed = true

        viewModelScope.launch {
            val result = captureHandler.captureLatestClipboard()
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    message = result.message,
                    savedCount = result.savedCount,
                    isSuccess = result.isSuccess,
                    shouldFinish = true
                )
            }
        }
    }
}