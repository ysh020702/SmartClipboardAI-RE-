package com.samsung.smartclipboard.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "topic_analysis_results",
    foreignKeys = [
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.Companion.CASCADE
        )
    ],
    indices = [Index("topicId")]
)
data class TopicAnalysisEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val topicId: Long,
    val summary: String,
    val keyPoints: String,
    val sourceItemIds: String,
    val createdAt: Long
)