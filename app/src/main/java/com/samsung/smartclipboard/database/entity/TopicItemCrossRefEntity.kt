package com.samsung.smartclipboard.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "topic_item_cross_refs",
    primaryKeys = ["topicId", "itemId"],
    foreignKeys = [
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.Companion.CASCADE
        ),
        ForeignKey(
            entity = DataItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.Companion.CASCADE
        )
    ],
    indices = [
        Index("topicId"),
        Index("itemId")
    ]
)
data class TopicItemCrossRefEntity(
    val topicId: Long,
    val itemId: Long,
    val addedAt: Long,
    val addedBy: String
)