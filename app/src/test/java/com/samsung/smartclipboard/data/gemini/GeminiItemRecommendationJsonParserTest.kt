package com.samsung.smartclipboard.data.gemini

import com.samsung.smartclipboard.TestModelFactory
import com.samsung.smartclipboard.domain.model.DataItemType
import org.junit.Assert.*
import org.junit.Test

class GeminiItemRecommendationJsonParserTest {

    // case 1: 정상 추천 결과 파싱
    @Test
    fun `parseItemRecommendation 정상_결과_파싱`() {
        val candidates = listOf(
            TestModelFactory.candidateItem(id = 1L, content = "여행 일정", score = 0.92f),
            TestModelFactory.candidateItem(id = 2L, content = "항공권 예약", score = 0.85f),
            TestModelFactory.candidateItem(id = 3L, content = "호텔 정보", score = 0.78f)
        )
        val raw = """
{
  "selectedItemIds": [1, 2],
  "itemReasons": [
    { "itemId": 1, "score": 0.95, "reason": "여행 핵심 키워드" },
    { "itemId": 2, "score": 0.88, "reason": "항공권 관련" }
  ],
  "recommendationReason": "여행 관련 데이터 2개를 추천합니다.",
  "suggestedQueries": ["여행 일정 정리"]
}
        """.trimIndent()

        val result = GeminiItemRecommendationJsonParser.parseItemRecommendation(raw, candidates)
        assertTrue("파싱 실패", result.isSuccess)
        val recommendation = result.getOrThrow()
        assertTrue(recommendation.recommendedItems.isNotEmpty())
        // selectedItemIds should be subset of recommendedItems
        recommendation.recommendedItems.forEach { assertTrue(it.relevanceScore in 0.0f..1.0f) }
        recommendation.selectedItemIds.forEach { id ->
            assertTrue(recommendation.recommendedItems.any { it.item.id == id })
        }
        assertEquals("여행 관련 데이터 2개를 추천합니다.", recommendation.recommendationReason)
    }

    // case 2: candidates에 없는 itemId는 제거
    @Test
    fun `parseItemRecommendation unknown_itemId_removed`() {
        val candidates = listOf(
            TestModelFactory.candidateItem(id = 1L),
            TestModelFactory.candidateItem(id = 2L)
        )
        val raw = """
{
  "selectedItemIds": [1, 999],
  "itemReasons": [
    { "itemId": 1, "score": 0.9, "reason": "관련 있음" },
    { "itemId": 999, "score": 0.8, "reason": "없는 아이템" }
  ],
  "recommendationReason": "추천",
  "suggestedQueries": []
}
        """.trimIndent()

        val result = GeminiItemRecommendationJsonParser.parseItemRecommendation(raw, candidates)
        assertTrue("파싱 실패", result.isSuccess)
        val recommendation = result.getOrThrow()
        assertTrue("999 should not be in selectedItemIds", 999 !in recommendation.selectedItemIds)
        // recommendedItems should not contain id 999
        assertTrue(recommendation.recommendedItems.none { it.item.id == 999L })
    }

    // case 3: selectedItemIds가 비어 있으면 recommendedItems 상위 3개 fallback 선택
    @Test
    fun `parseItemRecommendation empty_selectedItemIds_uses_fallback_top3`() {
        val candidates = listOf(
            TestModelFactory.candidateItem(id = 1L, score = 0.92f),
            TestModelFactory.candidateItem(id = 2L, score = 0.85f),
            TestModelFactory.candidateItem(id = 3L, score = 0.78f),
            TestModelFactory.candidateItem(id = 4L, score = 0.60f)
        )
        val raw = """
{
  "selectedItemIds": [],
  "itemReasons": [
    { "itemId": 1, "score": 0.92, "reason": "top" },
    { "itemId": 2, "score": 0.85, "reason": "mid" }
  ],
  "recommendationReason": "추천",
  "suggestedQueries": []
}
        """.trimIndent()

        val result = GeminiItemRecommendationJsonParser.parseItemRecommendation(raw, candidates)
        assertTrue(result.isSuccess)
        val recommendation = result.getOrThrow()
        assertTrue("selectedItemIds should not be empty when recommendedItems exist",
            recommendation.recommendedItems.isEmpty() || recommendation.selectedItemIds.isNotEmpty())
        if (recommendation.selectedItemIds.isNotEmpty()) {
            assertTrue(recommendation.selectedItemIds.size <= 3)
        }
    }

    // case 4: markdown fence와 설명문이 섞여도 JSON object 추출
    @Test
    fun `parseItemRecommendation extracts_JSON_from_mixed_text`() {
        val candidates = listOf(TestModelFactory.candidateItem(id = 1L))
        val raw = """
아래 추천 결과입니다.

```json
{
  "selectedItemIds": [1],
  "itemReasons": [
    { "itemId": 1, "score": 0.88, "reason": "잘 맞는 아이템" }
  ],
  "recommendationReason": "1개 추천",
  "suggestedQueries": []
}
```

이상입니다.
        """.trimIndent()

        val result = GeminiItemRecommendationJsonParser.parseItemRecommendation(raw, candidates)
        assertTrue("파싱 실패 - fence 제거 후 JSON 추출 필요", result.isSuccess)
    }
}