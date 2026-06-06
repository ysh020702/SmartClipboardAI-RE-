package com.samsung.smartclipboard.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class LlmStructuredOutput(
    val title: String,
    val topic: String,
    val purpose: String,
    val summary: String,
    val keywords: List<String>,
    val cleanedContent: String,
    val groupKey: String,
    val groupReason: String
)