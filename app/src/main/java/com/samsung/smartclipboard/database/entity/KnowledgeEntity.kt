package com.samsung.smartclipboard.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.samsung.smartclipboard.domain.model.InputType

@Entity(tableName = "knowledge_table")
data class KnowledgeEntity(
    @PrimaryKey val id: String,
    val type: InputType,
    val source: String,
    val title: String,
    val topic: String,
    val purpose: String,
    val summary: String,
    val keywords: List<String>,
    val content: String,
    val groupKey: String,
    val groupReason: String,
    val createdAt: Long
)