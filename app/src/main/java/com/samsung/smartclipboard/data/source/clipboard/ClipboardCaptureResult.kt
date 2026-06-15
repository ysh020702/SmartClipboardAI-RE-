package com.samsung.smartclipboard.data.source.clipboard

data class ClipboardCaptureResult(
    val isSuccess: Boolean,
    val savedCount: Int,
    val message: String,
    val type: CaptureResultType = CaptureResultType.TEXT_SAVED
)

enum class CaptureResultType {
    TEXT_SAVED,       // 텍스트 저장 성공
    LINK_SAVED,      // 링크 저장 성공
    EMPTY_CLIPBOARD, // 클립보드 비어 있음
    ALREADY_SAVED,   // 중복 (이미 저장됨)
    SAVE_FAILED      // 저장 실패
}
