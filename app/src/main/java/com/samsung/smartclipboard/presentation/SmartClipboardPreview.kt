package com.samsung.smartclipboard.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.samsung.smartclipboard.presentation.main.SmartClipboardAIApp

@Preview(showBackground = true)
@Composable
fun SmartClipboardPreview() {
    SmartClipboardTheme {
        SmartClipboardAIApp()
    }
}
