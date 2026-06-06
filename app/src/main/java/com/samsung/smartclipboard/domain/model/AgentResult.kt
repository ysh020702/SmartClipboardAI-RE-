package com.samsung.smartclipboard.domain.model

/**
 * TopicAgent.analyze()의 최종 출력.
 * TopicAnalysisEntity 1개 + TopicActionEntity N개로 분해되어 저장됩니다.
 */
data class AgentResult(
    val topicId: Long,
    val summary: String,
    val keyPoints: List<String>,
    val sourceItemIds: List<Long>,
    val actions: List<AgentActionDraft>
)

/**
 * TopicAgent가 추천한 개별 action 초안.
 * TopicActionEntity로 변환되어 저장됩니다.
 */
data class AgentActionDraft(
    val type: TopicActionType,
    val confidence: Float,
    val reason: String,
    val title: String,
    val body: String,
    val payload: String?,
    val sourceItemIds: List<Long>
)