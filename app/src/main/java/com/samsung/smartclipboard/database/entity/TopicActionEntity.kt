package com.samsung.smartclipboard.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.samsung.smartclipboard.database.entity.TopicAnalysisEntity

@Entity(
    tableName = "topic_actions",
    foreignKeys = [
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.Companion.CASCADE
        ),
        ForeignKey(
            entity = TopicAnalysisEntity::class,
            parentColumns = ["id"],
            childColumns = ["analysisResultId"],
            onDelete = ForeignKey.Companion.SET_NULL
        )
    ],
    indices = [
        Index("topicId"),
        Index("analysisResultId")
    ]
)
data class TopicActionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val topicId: Long,
    val analysisResultId: Long?,
    val type: String,
    val title: String,
    val body: String,
    val status: String,
    val editablePayload: String?,
    val createdAt: Long,
    val updatedAt: Long
)