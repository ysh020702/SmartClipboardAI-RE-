package com.samsung.smartclipboard.domain.model

data class TopicAnalysis(
    val id: Long,
    val topicId: Long,
    val summary: String,
    val keyPoints: List<String>,
    val sourceItemIds: List<Long>,
    val createdAt: Long
)
