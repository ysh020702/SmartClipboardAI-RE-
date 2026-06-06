package com.samsung.smartclipboard.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "data_items")
data class DataItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val type: String,
    val content: String,
    val title: String? = null,
    val source: String? = null,
    val mimeType: String? = null,
    val createdAt: Long,
    val extractedContent: String? = null,
    /** 이 정보를 찾은 사람이 왜 찾았을까를 나타내는 목적 (문장 형태) */
    val purpose: String? = null,
    /** purpose의 키워드 단위 추출 (콤마 구분), 유사도 분석 및 클러스터링에 사용 */
    val purposeKeyword: String? = null
)