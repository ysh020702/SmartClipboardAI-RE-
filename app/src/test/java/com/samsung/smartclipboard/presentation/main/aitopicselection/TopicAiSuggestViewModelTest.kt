package com.samsung.smartclipboard.presentation.main.aitopicselection

import com.samsung.smartclipboard.domain.model.DataCluster
import com.samsung.smartclipboard.domain.model.SuggestedTopic as DomainSuggestedTopic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopicAiSuggestViewModelTest {

    @Test
    fun `cluster topic candidates map to stable suggestion ui models`() {
        val cluster = DataCluster(
            clusterId = "cluster_1",
            clusterLabel = "여행 자료",
            itemIds = listOf(10L, 20L, 30L),
            topicCandidates = listOf(
                DomainSuggestedTopic(
                    suggestedTitle = "제주 여행 일정을 정리해줘",
                    description = "항공권과 숙소 정보를 바탕으로 일정을 정리합니다.",
                    confidence = 0.86f,
                    reason = "여행 관련 자료가 같은 묶음에 있습니다.",
                    relatedClusterId = "cluster_1"
                )
            )
        )

        val suggestions = listOf(cluster).toTopicAiSuggestionUi()

        assertEquals(1, suggestions.size)
        assertEquals("cluster_1:0", suggestions.first().id)
        assertEquals("제주 여행 일정을 정리해줘", suggestions.first().title)
        assertEquals("여행 자료", suggestions.first().clusterLabel)
        assertEquals(3, suggestions.first().itemCount)
        assertEquals(listOf(10L, 20L, 30L), suggestions.first().itemIds)
    }

    @Test
    fun `clusters without topic candidates fall back to cluster label suggestion`() {
        val cluster = DataCluster(
            clusterId = "cluster_2",
            clusterLabel = "회의 메모",
            itemIds = listOf(7L)
        )

        val suggestions = listOf(cluster).toTopicAiSuggestionUi()

        assertEquals(1, suggestions.size)
        assertEquals("회의 메모 정리해줘", suggestions.first().title)
        assertTrue(suggestions.first().confidence in 0.0f..1.0f)
        assertEquals("cluster_2", suggestions.first().relatedClusterId)
    }

    @Test
    fun `suggestion ui maps to card metadata`() {
        val suggestion = TopicAiSuggestionUi(
            id = "cluster_3:0",
            title = "쇼핑 후보를 비교해줘",
            description = "상품 링크와 캡처를 비교합니다.",
            confidence = 0.72f,
            reason = "상품 정보가 모여 있습니다.",
            clusterLabel = "쇼핑 자료",
            itemCount = 4,
            itemIds = listOf(1L, 2L, 3L, 4L),
            relatedClusterId = "cluster_3"
        )

        val card = suggestion.toTopicAiSuggestionCardUi()

        assertEquals("cluster_3:0", card.id)
        assertEquals("쇼핑 후보를 비교해줘", card.title)
        assertEquals(listOf("자료 4개", "신뢰도 72%"), card.dataTypes)
        assertEquals(listOf("쇼핑 자료"), card.tags)
    }

    @Test
    fun `creating action state blocks additional suggestion selection`() {
        val state = TopicAiSuggestUiState(
            isCreatingTopic = true,
            selectedSuggestionId = "cluster_3:0"
        )

        assertFalse(state.canSelectSuggestion)
        assertTrue(state.isCreatingSuggestion("cluster_3:0"))
        assertFalse(state.isCreatingSuggestion("cluster_4:0"))
    }

    @Test
    fun `home ai recommend launch starts with loading content before viewmodel updates`() {
        val state = TopicAiSuggestUiState()

        val content = resolveTopicAiSuggestContent(
            uiState = state,
            preferInitialLoading = true,
        )

        assertEquals(TopicAiSuggestContent.Loading, content)
    }
}
