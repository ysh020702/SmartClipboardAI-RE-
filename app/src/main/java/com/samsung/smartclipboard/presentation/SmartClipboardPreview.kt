package com.samsung.smartclipboard.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.samsung.smartclipboard.presentation.main.MainScreen

@Preview(showBackground = true)
@Composable
fun SmartClipboardPreview() {
    SmartClipboardTheme {
        MainScreen()
    }
}
