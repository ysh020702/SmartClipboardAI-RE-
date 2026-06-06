package com.samsung.smartclipboard.domain.model

/**
 * 클러스터링 결과 DTO.
 *
 * @property clusterId 클러스터 식별자
 * @property clusterLabel 클러스터 표시 레이블
 * @property itemIds 클러스터에 속한 DataItem ID 목록
 * @property topicCandidates 이 클러스터에서 파생된 추천 주제 목록
 * @property generatedAt 클러스터링 실행 시각 (epoch millis)
 */
data class DataCluster(
    val clusterId: String,
    val clusterLabel: String,
    val itemIds: List<Long>,
    val topicCandidates: List<SuggestedTopic> = emptyList(),
    val generatedAt: Long = System.currentTimeMillis()
)