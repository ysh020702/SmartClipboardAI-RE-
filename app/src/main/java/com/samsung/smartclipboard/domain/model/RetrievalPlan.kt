package com.samsung.smartclipboard.domain.model

import com.samsung.smartclipboard.domain.model.DataItemType

/**
 * LLM TopicPlanner가 생성할 검색 계획 DTO.
 *
 * @property keywords 검색 키워드 목록
 * @property typeFilters 검색 대상 DataItemType 필터. emptyList()이면 모든 타입 허용.
 * @property dateRangeDays 최근 N일 이내 검색. null이면 무제한.
 * @property maxResults 최대 결과 개수
 */
data class RetrievalPlan(
    val keywords: List<String> = emptyList(),
    val typeFilters: List<DataItemType> = emptyList(),
    val dateRangeDays: Int? = null,
    val maxResults: Int = 20
)