package com.samsung.smartclipboard

import com.samsung.smartclipboard.domain.model.AgentActionDraft
import com.samsung.smartclipboard.domain.model.CandidateItem
import com.samsung.smartclipboard.domain.model.DataCluster
import com.samsung.smartclipboard.domain.model.DataItem
import com.samsung.smartclipboard.domain.model.DataItemType
import com.samsung.smartclipboard.domain.model.RetrievalPlan
import com.samsung.smartclipboard.domain.model.TopicActionType
import com.samsung.smartclipboard.presentation.SuggestedTopic

object TestModelFactory {
    fun dataItem(
        id: Long = 1L,
        type: DataItemType = DataItemType.TEXT,
        content: String = "테스트 콘텐츠",
        title: String? = "테스트 제목",
        source: String? = "test",
        mimeType: String? = "text/plain",
        createdAt: Long = 1_700_000_000_000L
    ): DataItem = DataItem(
        id = id,
        type = type,
        content = content,
        title = title,
        source = source,
        mimeType = mimeType,
        createdAt = createdAt
    )

    fun candidateItem(
        id: Long = 1L,
        content: String = "테스트 콘텐츠",
        score: Float = 0.8f,
        reason: String = "키워드 매칭"
    ): CandidateItem = CandidateItem(
        item = dataItem(id = id, content = content),
        relevanceScore = score,
        relevanceReason = reason
    )

    fun retrievalPlan(
        keywords: List<String> = listOf("테스트"),
        typeFilters: List<DataItemType> = emptyList(),
        dateRangeDays: Int? = null,
        maxResults: Int = 20
    ): RetrievalPlan = RetrievalPlan(
        keywords = keywords,
        typeFilters = typeFilters,
        dateRangeDays = dateRangeDays,
        maxResults = maxResults
    )

    fun actionDraft(
        type: TopicActionType = TopicActionType.SUMMARY,
        sourceItemIds: List<Long> = listOf(1L),
        title: String = "테스트 작업",
        body: String = "테스트 본문",
        confidence: Float = 0.8f,
        reason: String = "테스트 이유",
        payload: String? = null
    ): AgentActionDraft = AgentActionDraft(
        type = type,
        confidence = confidence,
        reason = reason,
        title = title,
        body = body,
        payload = payload,
        sourceItemIds = sourceItemIds
    )

    fun dataCluster(
        clusterId: String = "cluster_0_1",
        clusterLabel: String = "테스트 묶음",
        itemIds: List<Long> = listOf(1L, 2L),
        topicCandidates: List<SuggestedTopic> = emptyList(),
        generatedAt: Long = 1_700_000_000_000L
    ): DataCluster = DataCluster(
        clusterId = clusterId,
        clusterLabel = clusterLabel,
        itemIds = itemIds,
        topicCandidates = topicCandidates,
        generatedAt = generatedAt
    )

    fun suggestedTopic(
        suggestedTitle: String = "추천 주제",
        description: String = "추천 설명",
        confidence: Float = 0.75f,
        reason: String = "추천 이유",
        relatedClusterId: String? = null
    ): SuggestedTopic = SuggestedTopic(
        suggestedTitle = suggestedTitle,
        description = description,
        confidence = confidence,
        reason = reason,
        relatedClusterId = relatedClusterId
    )
}