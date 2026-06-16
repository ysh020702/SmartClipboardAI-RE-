package com.samsung.smartclipboard.domain.model

enum class TaskSelectionType {
    SUMMARY,
    CALENDAR,
    REMINDER,
    SHARE_DRAFT
}

enum class TaskSelectionStatus {
    DRAFT,
    EDITED,
    EXECUTED,
    DISMISSED
}

data class TaskSelection(
    val id: Long,
    val topicId: Long,
    val analysisResultId: Long?,
    val type: TaskSelectionType,
    val title: String,
    val body: String,
    val status: TaskSelectionStatus,
    val editablePayload: String?,
    val createdAt: Long,
    val updatedAt: Long
)
