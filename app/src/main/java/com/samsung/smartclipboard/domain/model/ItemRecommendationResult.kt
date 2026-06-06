package com.samsung.smartclipboard.domain.model

/**
 * ItemRecommendationAgent의 결과 DTO.
 *
 * M5 AgentSessionViewModel에서 AwaitingItemSelection 상태 구성 시 사용한다.
 *
 * @property recommendedItems 정렬된 추천 CandidateItem 목록 (최대 10개)
 * @property selectedItemIds 기본 선택된 itemId 집합. recommendedItems에 포함된 id만 허용.
 * @property recommendationReason 추천 이유 설명 (한국어 1~3문장)
 * @property suggestedQueries 추가 검색어 제안 (0~5개)
 */
data class ItemRecommendationResult(
    val recommendedItems: List<CandidateItem>,
    val selectedItemIds: Set<Long>,
    val recommendationReason: String,
    val suggestedQueries: List<String> = emptyList()
)