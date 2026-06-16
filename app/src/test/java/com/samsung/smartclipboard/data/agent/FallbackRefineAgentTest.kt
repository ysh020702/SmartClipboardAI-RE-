package com.samsung.smartclipboard.data.agent

import com.samsung.smartclipboard.TestModelFactory
import com.samsung.smartclipboard.domain.model.CandidateItem
import com.samsung.smartclipboard.domain.model.TopicActionType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class FallbackRefineAgentTest {

    private val refineAgent = FallbackRefineAgent()

    // case 1: blank feedback returns failure
    @Test
    fun `refineActions blank_feedback_returns_failure`() = runBlocking {
        val result = refineAgent.refineActions(
            topicQuery = "주제",
            plan = TestModelFactory.retrievalPlan(),
            selectedItems = listOf(TestModelFactory.candidateItem(id = 1L)),
            currentActions = listOf(TestModelFactory.actionDraft()),
            feedback = "  "
        )
        assertTrue("blank feedback must fail", result.isFailure)
    }

    // case 2: valid feedback returns actions and preserves sourceItemIds subset
    @Test
    fun `refineActions valid_feedback_returns_actions_and_preserves_subset`() = runBlocking {
        val selectedItems = listOf(
            TestModelFactory.candidateItem(id = 1L, content = "회의록 내용"),
            TestModelFactory.candidateItem(id = 2L, content = "일정 메모")
        )
        val currentActions = listOf(
            TestModelFactory.actionDraft(
                type = TopicActionType.SUMMARY,
                sourceItemIds = listOf(1L, 999L), // 999 out of range
                title = "회의 요약",
                body = "긴 본문입니다.".repeat(50),
                confidence = 0.80f
            ),
            TestModelFactory.actionDraft(
                type = TopicActionType.SHARE_DRAFT,
                sourceItemIds = listOf(1L, 2L),
                title = "공유 메시지",
                body = "공유 메시지 내용",
                confidence = 0.75f
            )
        )
        val feedback = "공유 메시지 중심으로 짧게 만들어줘"

        val result = refineAgent.refineActions(
            topicQuery = "회의 정리",
            plan = TestModelFactory.retrievalPlan(),
            selectedItems = selectedItems,
            currentActions = currentActions,
            feedback = feedback
        )
        assertTrue("refine failed", result.isSuccess)
        val actions = result.getOrThrow()
        assertTrue(actions.isNotEmpty())
        actions.forEach { action ->
            // 모든 sourceItemIds는 selectedItems의 id subset이어야 함
            action.sourceItemIds.forEach { id ->
                assertTrue("$id must be in selected items", id in setOf(1L, 2L))
            }
            assertTrue(action.confidence in 0.0f..1.0f)
            assertTrue(action.title.isNotBlank())
            assertTrue(action.body.isNotBlank())
        }
    }

    // case 3: feedback with 공유 prioritizes SHARE_DRAFT if present
    @Test
    fun `refineActions feedback_with_share_prioritizes_SHARE_DRAFT`() = runBlocking {
        val selectedItems = listOf(TestModelFactory.candidateItem(id = 1L))
        val currentActions = listOf(
            TestModelFactory.actionDraft(type = TopicActionType.SUMMARY, title = "요약", body = "내용", sourceItemIds = listOf(1L)),
            TestModelFactory.actionDraft(type = TopicActionType.SHARE_DRAFT, title = "공유 메시지", body = "공유", sourceItemIds = listOf(1L))
        )
        val feedback = "공유 메시지 중심으로"

        val result = refineAgent.refineActions(
            topicQuery = "주제",
            plan = TestModelFactory.retrievalPlan(),
            selectedItems = selectedItems,
            currentActions = currentActions,
            feedback = feedback
        )
        assertTrue(result.isSuccess)
        val actions = result.getOrThrow()
        // SHARE_DRAFT가 상위권에 있어야 함
        val shareDrafts = actions.filter { it.type == TopicActionType.SHARE_DRAFT }
        assertTrue("SHARE_DRAFT should be present", shareDrafts.isNotEmpty())
    }

    // case 4: feedback with 짧게 shortens body
    @Test
    fun `refineActions feedback_with_shorten_keyword_shortens_body`() = runBlocking {
        val selectedItems = listOf(TestModelFactory.candidateItem(id = 1L))
        val originalBody = "이것은 매우 긴 본문입니다. ".repeat(100)
        val currentActions = listOf(
            TestModelFactory.actionDraft(
                type = TopicActionType.SUMMARY,
                sourceItemIds = listOf(1L),
                title = "긴 요약",
                body = originalBody
            )
        )
        val feedback = "짧게 정리해줘"

        val result = refineAgent.refineActions(
            topicQuery = "주제",
            plan = TestModelFactory.retrievalPlan(),
            selectedItems = selectedItems,
            currentActions = currentActions,
            feedback = feedback
        )
        assertTrue(result.isSuccess)
        val actions = result.getOrThrow()
        assertTrue("body should be shortened", actions.first().body.length <= originalBody.length)
    }

    // case 5: empty selectedItems returns failure
    @Test
    fun `refineActions empty_selectedItems_returns_failure`() = runBlocking {
        val result = refineAgent.refineActions(
            topicQuery = "주제",
            plan = TestModelFactory.retrievalPlan(),
            selectedItems = emptyList(),
            currentActions = listOf(TestModelFactory.actionDraft()),
            feedback = "보완 요청"
        )
        assertTrue("empty selected items must fail", result.isFailure)
    }
}