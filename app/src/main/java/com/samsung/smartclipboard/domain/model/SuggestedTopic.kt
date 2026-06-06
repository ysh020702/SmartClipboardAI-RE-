package com.samsung.smartclipboard.domain.model

/**
 * Flow B에서 생성하는 추천 주제 DTO.
 *
 * @property suggestedTitle 추천 주제 제목
 * @property description 주제 설명
 * @property confidence 신뢰도 점수 (0.0f ~ 1.0f)
 * @property reason 추천 이유 설명
 * @property relatedClusterId 연관 클러스터 ID. null인 경우 특정 클러스터에 속하지 않음.
 */
data class SuggestedTopic(
    val suggestedTitle: String,
    val description: String,
    val confidence: Float,
    val reason: String,
    val relatedClusterId: String? = null
)