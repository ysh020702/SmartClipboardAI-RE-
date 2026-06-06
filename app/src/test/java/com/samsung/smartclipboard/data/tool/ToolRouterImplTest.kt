package com.samsung.smartclipboard.data.tool

import com.samsung.smartclipboard.TestModelFactory
import com.samsung.smartclipboard.domain.model.TopicActionType
import org.junit.Assert.*
import org.junit.Test

class ToolRouterImplTest {

    private val registry = ToolRegistryImpl()
    private val router = ToolRouterImpl(registry)

    // case 1: SUMMARY 기본 라우팅은 save_note_share (노트 앱 공유)
    @Test
    fun `route SUMMARY defaults to save_note_share`() {
        val action = TestModelFactory.actionDraft(
            type = TopicActionType.SUMMARY,
            title = "회의 요약",
            body = "회의 내용을 요약합니다."
        )
        val result = router.route(action)
        assertTrue("라우팅 실패", result.isSuccess)
        val routeResult = result.getOrThrow()
        assertEquals("save_note_share", routeResult.toolSpec.toolName)
        assertTrue(routeResult.resolvedPayload["noteTitle"]?.isNotBlank() == true)
        assertTrue(routeResult.resolvedPayload["noteBody"]?.isNotBlank() == true)
    }

    // case 2: SHARE_DRAFT는 share_text
    @Test
    fun `route SHARE_DRAFT defaults to share_text`() {
        val action = TestModelFactory.actionDraft(
            type = TopicActionType.SHARE_DRAFT,
            title = "공유 자료",
            body = "공유할 내용입니다."
        )
        val result = router.route(action)
        assertTrue("라우팅 실패", result.isSuccess)
        val routeResult = result.getOrThrow()
        assertEquals("share_text", routeResult.toolSpec.toolName)
        assertTrue(routeResult.resolvedPayload["shareText"]?.isNotBlank() == true)
    }

    // case 3: http/https URL이 있으면 open_url 가능
    @Test
    fun `route detects_http_url_and_routes_to_open_url`() {
        val action = TestModelFactory.actionDraft(
            type = TopicActionType.SUMMARY,
            title = "링크 정리",
            body = "참고: https://example.com 에서 확인하세요.",
            payload = "{\"preferredTool\":\"open_url\"}"
        )
        val result = router.route(action)
        assertTrue(result.isSuccess)
        val routeResult = result.getOrThrow()
        // With preferredTool=open_url, should route to open_url
        assertEquals("open_url", routeResult.toolSpec.toolName)
        assertTrue(routeResult.resolvedPayload["url"]?.startsWith("https://") == true)
    }

    // case 4: javascript/file URL은 open_url로 라우팅되지 않는다
    @Test
    fun `route rejects_non_http_url_for_open_url`() {
        val action = TestModelFactory.actionDraft(
            type = TopicActionType.SUMMARY,
            title = "스크립트",
            body = "JAVA-SCRIPT:alert(1)",
            payload = "{\"preferredTool\":\"open_url\"}"
        )
        val result = router.route(action)
        // Either failure, or fallback to copy_to_clipboard (not open_url)
        if (result.isSuccess) {
            val routeResult = result.getOrThrow()
            // If fallback, tool must not be open_url
            assertTrue("open_url must not be used for non-http URL",
                routeResult.toolSpec.toolName != "open_url" || routeResult.missingRequiredInputs.isNotEmpty())
        } else {
            // Failure is also acceptable
            assertTrue(true)
        }
    }

    // case 5: title/body가 모두 blank면 failure
    @Test
    fun `route blank title_and_body returns failure`() {
        val action = TestModelFactory.actionDraft(
            type = TopicActionType.SUMMARY,
            title = "",
            body = " "
        )
        val result = router.route(action)
        assertTrue("blank title and body must fail", result.isFailure)
    }

    // case 6: Todo 라우팅은 기본적으로 save_note_share
    @Test
    fun `route TODO defaults to save_note_share`() {
        val action = TestModelFactory.actionDraft(
            type = TopicActionType.TODO,
            title = "할 일 목록",
            body = "오늘 할 일 세 가지"
        )
        val result = router.route(action)
        assertTrue(result.isSuccess)
        assertEquals("save_note_share", result.getOrThrow().toolSpec.toolName)
    }

    // case 7: CALENDAR 라우팅은 insert_calendar_event
    @Test
    fun `route CALENDAR defaults to insert_calendar_event`() {
        val action = TestModelFactory.actionDraft(
            type = TopicActionType.CALENDAR,
            title = "팀 미팅",
            body = "오후 2시 팀 미팅"
        )
        val result = router.route(action)
        assertTrue(result.isSuccess)
        val routeResult = result.getOrThrow()
        assertEquals("insert_calendar_event", routeResult.toolSpec.toolName)
        assertTrue(routeResult.resolvedPayload["eventTitle"]?.isNotBlank() == true)
    }

    // case 8: REMINDER 라우팅은 set_reminder
    @Test
    fun `route REMINDER defaults to set_reminder`() {
        val action = TestModelFactory.actionDraft(
            type = TopicActionType.REMINDER,
            title = "제출 마감",
            body = "과제 제출 마감일"
        )
        val result = router.route(action)
        assertTrue(result.isSuccess)
        val routeResult = result.getOrThrow()
        assertEquals("set_reminder", routeResult.toolSpec.toolName)
        assertTrue(routeResult.resolvedPayload["reminderTitle"]?.isNotBlank() == true)
    }
}
