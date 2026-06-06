package com.samsung.smartclipboard.data.gemini

import com.samsung.smartclipboard.TestModelFactory
import com.samsung.smartclipboard.domain.model.DataItemType
import com.samsung.smartclipboard.domain.model.TopicActionType
import org.junit.Assert.*
import org.junit.Test

class GeminiActionPlannerJsonParserTest {

    // case 1: markdown fence가 있는 정상 JSON 파싱
    @Test
    fun `parseActions 정상_JSON_with_fence`() {
        val selectedItems = listOf(
            TestModelFactory.candidateItem(id = 1L),
            TestModelFactory.candidateItem(id = 2L)
        )
        val raw = """
```json
{
  "actions": [
    {
      "type": "SUMMARY",
      "confidence": 0.91,
      "reason": "요약에 적합합니다.",
      "title": "자료 요약",
      "body": "핵심 내용을 요약합니다.",
      "payload": {},
      "sourceItemIds": [1, 2]
    }
  ]
}
```
        """.trimIndent()

        val result = GeminiActionPlannerJsonParser.parseActions(raw, selectedItems)
        assertTrue("파싱 실패", result.isSuccess)
        val actions = result.getOrThrow()
        assertEquals(1, actions.size)
        assertEquals(TopicActionType.SUMMARY, actions[0].type)
        assertTrue(actions[0].confidence in 0.0f..1.0f)
        assertEquals("자료 요약", actions[0].title)
        assertTrue(actions[0].body.isNotBlank())
    }

    // case 2: unknown type은 무시
    @Test
    fun `parseActions unknown_type_must_be_ignored`() {
        val selectedItems = listOf(TestModelFactory.candidateItem(id = 1L))
        val raw = """
{
  "actions": [
    {
      "type": "DELETE_FILE",
      "confidence": 0.9,
      "reason": "파일 삭제",
      "title": "파일 삭제",
      "body": "삭제합니다",
      "payload": {},
      "sourceItemIds": [1]
    },
    {
      "type": "SUMMARY",
      "confidence": 0.88,
      "reason": "요약",
      "title": "요약 작업",
      "body": "요약 초안",
      "payload": {},
      "sourceItemIds": [1]
    }
  ]
}
        """.trimIndent()

        val result = GeminiActionPlannerJsonParser.parseActions(raw, selectedItems)
        assertTrue("파싱 실패", result.isSuccess)
        val actions = result.getOrThrow()
        assertTrue(actions.none { it.title == "파일 삭제" })
        assertEquals("요약 작업", actions.first().title)
    }

    // case 3: sourceItemIds가 selectedItems 밖이면 제거
    @Test
    fun `parseActions out_of_range_sourceItemIds_are_removed`() {
        val selectedItems = listOf(
            TestModelFactory.candidateItem(id = 1L),
            TestModelFactory.candidateItem(id = 2L)
        )
        val raw = """
{
  "actions": [
    {
      "type": "SUMMARY",
      "confidence": 0.85,
      "reason": "요약",
      "title": "요약",
      "body": "내용",
      "payload": {},
      "sourceItemIds": [1, 999]
    }
  ]
}
        """.trimIndent()

        val result = GeminiActionPlannerJsonParser.parseActions(raw, selectedItems)
        assertTrue(result.isSuccess)
        val actions = result.getOrThrow()
        val sourceIds = actions.first().sourceItemIds
        assertTrue("999 should be removed", 999 !in sourceIds)
        assertTrue("1 should remain", 1L in sourceIds)
    }

    // case 4: confidence 범위 coerce
    @Test
    fun `parseActions confidence_coerced_to_1f`() {
        val selectedItems = listOf(TestModelFactory.candidateItem(id = 1L))
        val raw = """
{
  "actions": [
    {
      "type": "SUMMARY",
      "confidence": 9.9,
      "reason": "요약",
      "title": "요약",
      "body": "내용",
      "payload": {},
      "sourceItemIds": [1]
    }
  ]
}
        """.trimIndent()

        val result = GeminiActionPlannerJsonParser.parseActions(raw, selectedItems)
        assertTrue(result.isSuccess)
        val actions = result.getOrThrow()
        assertTrue("confidence must be <= 1.0f", actions.first().confidence <= 1.0f)
    }

    // case 5: 빈 sourceItemIds는 fallback 처리
    @Test
    fun `parseActions empty_sourceItemIds_gets_fallback`() {
        val selectedItems = listOf(
            TestModelFactory.candidateItem(id = 1L),
            TestModelFactory.candidateItem(id = 2L),
            TestModelFactory.candidateItem(id = 3L)
        )
        val raw = """
{
  "actions": [
    {
      "type": "SUMMARY",
      "confidence": 0.85,
      "reason": "요약",
      "title": "요약",
      "body": "내용",
      "payload": {},
      "sourceItemIds": []
    }
  ]
}
        """.trimIndent()

        val result = GeminiActionPlannerJsonParser.parseActions(raw, selectedItems)
        assertTrue(result.isSuccess)
        val actions = result.getOrThrow()
        // sourceItemIds should be non-empty after parser fallback
        assertTrue("sourceItemIds must not be empty", actions.first().sourceItemIds.isNotEmpty())
        actions.first().sourceItemIds.forEach { assertTrue(it in setOf(1L, 2L, 3L)) }
    }
}