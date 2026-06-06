package com.samsung.smartclipboard

import com.samsung.smartclipboard.domain.model.*
import com.samsung.smartclipboard.presentation.SuggestedTopic
import org.junit.Assert.*
import org.junit.Test

/**
 * 새 도메인 모델 DTO의 기본값 생성 검증.
 */
class DomainModelConstructionTest {

    // --- AgentSession ---

    @Test
    fun `AgentSession 기본값 생성`() {
        val session = AgentSession(
            sessionId = "s-001",
            topicTitle = "주간 업무 정리"
        )
        assertEquals("s-001", session.sessionId)
        assertEquals("주간 업무 정리", session.topicTitle)
        assertTrue(session.state is AgentSessionState.Idle)
        assertTrue(session.candidateItems.isEmpty())
        assertTrue(session.actionDrafts.isEmpty())
        assertNull(session.selectedActionIndex)
        assertTrue(session.toolResults.isEmpty())
        assertTrue(session.createdAt > 0)
        assertTrue(session.updatedAt > 0)
    }

    // --- CandidateItem ---

    @Test
    fun `CandidateItem 생성`() {
        val dataItem = DataItem(
            id = 1L,
            type = DataItemType.TEXT,
            content = "테스트 콘텐츠",
            title = "제목",
            source = "클립보드",
            mimeType = "text/plain",
            createdAt = System.currentTimeMillis()
        )
        val candidate = CandidateItem(
            item = dataItem,
            relevanceScore = 0.85f,
            relevanceReason = "키워드 매칭 3개"
        )
        assertEquals(dataItem, candidate.item)
        assertEquals(0.85f, candidate.relevanceScore)
        assertEquals("키워드 매칭 3개", candidate.relevanceReason)
    }

    @Test
    fun `CandidateItem relevanceScore는_0과_1_사이`() {
        val dataItem = DataItem(
            id = 1L,
            type = DataItemType.TEXT,
            content = "test",
            createdAt = System.currentTimeMillis()
        )
        val lowCandidate = CandidateItem(dataItem, 0.0f, "낮음")
        val highCandidate = CandidateItem(dataItem, 1.0f, "높음")
        assertEquals(0.0f, lowCandidate.relevanceScore)
        assertEquals(1.0f, highCandidate.relevanceScore)
    }

    // --- RetrievalPlan ---

    @Test
    fun `RetrievalPlan 기본값 생성`() {
        val plan = RetrievalPlan()
        assertTrue(plan.keywords.isEmpty())
        assertTrue(plan.typeFilters.isEmpty())
        assertNull(plan.dateRangeDays)
        assertEquals(20, plan.maxResults)
    }

    @Test
    fun `RetrievalPlan 커스텀값 생성`() {
        val plan = RetrievalPlan(
            keywords = listOf("회의", "일정"),
            typeFilters = listOf(DataItemType.TEXT, DataItemType.LINK),
            dateRangeDays = 7,
            maxResults = 10
        )
        assertEquals(2, plan.keywords.size)
        assertEquals(2, plan.typeFilters.size)
        assertEquals(7, plan.dateRangeDays)
        assertEquals(10, plan.maxResults)
    }

    // --- RequiredInput ---

    @Test
    fun `RequiredInput 기본값 생성`() {
        val input = RequiredInput(
            key = "noteTitle",
            label = "노트 제목"
        )
        assertEquals("noteTitle", input.key)
        assertEquals("노트 제목", input.label)
        assertNull(input.value)
        assertTrue(input.required)
    }

    @Test
    fun `RequiredInput 선택적_입력`() {
        val input = RequiredInput(
            key = "location",
            label = "위치",
            value = "서울",
            required = false
        )
        assertEquals("서울", input.value)
        assertFalse(input.required)
    }

    // --- ToolSpec ---

    @Test
    fun `ToolSpec 생성`() {
        val spec = ToolSpec(
            toolName = "save_note",
            description = "노트 저장",
            riskLevel = ToolRiskLevel.LOW,
            requiresConfirmation = false,
            androidAction = "com.samsung.smartclipboard.action.SAVE_NOTE",
            requiredInputs = listOf(
                RequiredInput("noteTitle", "제목"),
                RequiredInput("noteBody", "내용")
            )
        )
        assertEquals("save_note", spec.toolName)
        assertEquals(ToolRiskLevel.LOW, spec.riskLevel)
        assertFalse(spec.requiresConfirmation)
        assertEquals(2, spec.requiredInputs.size)
    }

    @Test
    fun `ToolSpec 고위험_확인_필요`() {
        val spec = ToolSpec(
            toolName = "share_text",
            description = "텍스트 공유",
            riskLevel = ToolRiskLevel.MEDIUM,
            requiresConfirmation = true,
            androidAction = "android.intent.action.SEND"
        )
        assertTrue(spec.requiresConfirmation)
    }

    // --- ToolExecutionResult ---

    @Test
    fun `ToolExecutionResult_success_생성`() {
        val result = ToolExecutionResult(
            resultId = "r-001",
            sessionId = "s-001",
            toolName = "save_note",
            success = true,
            message = "노트가 저장되었습니다"
        )
        assertTrue(result.success)
        assertEquals("노트가 저장되었습니다", result.message)
        assertNull(result.errorDetail)
    }

    @Test
    fun `ToolExecutionResult_failure_생성`() {
        val result = ToolExecutionResult(
            resultId = "r-002",
            sessionId = "s-001",
            toolName = "share_text",
            success = false,
            message = "공유 실패",
            errorDetail = "No activity found to handle intent"
        )
        assertFalse(result.success)
        assertEquals("No activity found to handle intent", result.errorDetail)
    }

    // --- SuggestedTopic ---

    @Test
    fun `SuggestedTopic 생성`() {
        val topic = SuggestedTopic(
            suggestedTitle = "주간 업무 보고서",
            description = "이번 주 수집된 업무 관련 노트 정리",
            confidence = 0.9f,
            reason = "업무 관련 키워드 15개 발견"
        )
        assertEquals("주간 업무 보고서", topic.suggestedTitle)
        assertEquals(0.9f, topic.confidence)
        assertNull(topic.relatedClusterId)
    }

    @Test
    fun `SuggestedTopic_클러스터_연관`() {
        val topic = SuggestedTopic(
            suggestedTitle = "쇼핑 리스트",
            description = "링크와 텍스트 기반 쇼핑 정보",
            confidence = 0.75f,
            reason = "쇼핑 관련 링크 5개",
            relatedClusterId = "cluster-003"
        )
        assertEquals("cluster-003", topic.relatedClusterId)
    }

    // --- DataCluster ---

    @Test
    fun `DataCluster 생성`() {
        val cluster = DataCluster(
            clusterId = "c-001",
            clusterLabel = "업무 관련",
            itemIds = listOf(1L, 2L, 3L)
        )
        assertEquals("c-001", cluster.clusterId)
        assertEquals("업무 관련", cluster.clusterLabel)
        assertEquals(3, cluster.itemIds.size)
        assertTrue(cluster.topicCandidates.isEmpty())
        assertTrue(cluster.generatedAt > 0)
    }

    @Test
    fun `DataCluster_추천주제_포함`() {
        val topic = SuggestedTopic(
            suggestedTitle = "업무 회의 정리",
            description = "회의록 기반 정리",
            confidence = 0.88f,
            reason = "회의록 3건"
        )
        val cluster = DataCluster(
            clusterId = "c-002",
            clusterLabel = "회의록",
            itemIds = listOf(10L, 11L),
            topicCandidates = listOf(topic)
        )
        assertEquals(1, cluster.topicCandidates.size)
        assertEquals("업무 회의 정리", cluster.topicCandidates[0].suggestedTitle)
    }
}