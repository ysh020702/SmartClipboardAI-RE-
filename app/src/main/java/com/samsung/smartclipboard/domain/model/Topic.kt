package com.samsung.smartclipboard.domain.model

data class Topic(
    val id: Long,
    val title: String,
    val itemCount: Int,
    val createdAt: Long,
    val updatedAt: Long
)