package com.samsung.smartclipboard.presentation.share

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.smartclipboard.data.source.share.ShareContentHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShareReceiverViewModel @Inject constructor(
    private val shareContentHandler: ShareContentHandler
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShareReceiverUiState())
    val uiState: StateFlow<ShareReceiverUiState> = _uiState

    private var processed = false

    fun processShareIntent(intent: Intent) {
        if (processed) return
        processed = true

        viewModelScope.launch {
            val result = shareContentHandler.saveFromIntent(intent)
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