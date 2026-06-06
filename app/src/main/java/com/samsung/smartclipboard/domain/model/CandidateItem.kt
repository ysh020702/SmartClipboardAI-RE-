package com.samsung.smartclipboard.domain.model

import com.samsung.smartclipboard.domain.model.DataItem

/**
 * 검색/추천된 DataItem wrapper.
 *
 * @property item 검색된 원본 DataItem
 * @property relevanceScore 관련성 점수 (0.0f ~ 1.0f). 실제 검증 로직은 M2 또는 parser 단계에서 구현.
 * @property relevanceReason 관련성 설명 문자열
 */
data class CandidateItem(
    val item: DataItem,
    val relevanceScore: Float,
    val relevanceReason: String
)