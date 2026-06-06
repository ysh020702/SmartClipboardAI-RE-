package com.samsung.smartclipboard.domain.model

enum class TopicActionType {
    SUMMARY,
    CALENDAR,
    REMINDER,
    SHARE_DRAFT,
    TODO
}

enum class TopicActionStatus {
    DRAFT,
    EDITED,
    EXECUTED,
    DISMISSED
}

data class TopicAction(
    val id: Long,
    val topicId: Long,
    val analysisResultId: Long?,
    val type: TopicActionType,
    val title: String,
    val body: String,
    val status: TopicActionStatus,
    val editablePayload: String?,
    val createdAt: Long,
    val updatedAt: Long
)
