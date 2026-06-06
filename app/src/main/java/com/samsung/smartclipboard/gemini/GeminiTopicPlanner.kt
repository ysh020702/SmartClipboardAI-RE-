package com.samsung.smartclipboard.gemini

import com.samsung.smartclipboard.domain.model.DataItemType
import com.samsung.smartclipboard.domain.model.RetrievalPlan
import com.samsung.smartclipboard.gemini.GeminiUtils.extractJsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class GeminiTopicPlanner(
    private val geminiManager: GeminiManager
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun plan(topicQuery: String): Result<RetrievalPlan> {
        val trimmed = topicQuery.trim()
        if (trimmed.isBlank()) return Result.failure(IllegalArgumentException("주제가 비어 있습니다"))

        return runCatching {
            // 1. 프롬프트 생성 및 Gemini 실행, 그리고 파싱
            val rawResponse = geminiManager.run(buildPrompt(trimmed))
            val plan = parseRetrievalPlan(rawResponse).getOrThrow()

            // 2. 결과 검증 (조건에 맞지 않으면 예외 발생 -> 폴백 유도)
            require(plan.keywords.isNotEmpty() && plan.keywords.size <= 8) { "키워드 갯수 오류" }
            require(plan.maxResults in 5..50) { "maxResults 범위 오류" }
            require(plan.dateRangeDays == null || plan.dateRangeDays in 1..365) { "dateRangeDays 범위 오류" }

            plan
        }
    }

    private fun buildPrompt(topicQuery: String): String = """
        너는 Android 로컬 데이터 검색 계획을 수립하는 비서다.
        사용자의 주제에 맞는 데이터를 Android 기기 내에서 찾기 위한 검색 계획을 JSON으로 출력해라.

        ## 반드시 지킬 규칙
        - 응답은 반드시 JSON object 하나만 출력한다.
        - markdown 코드 펜스(```), 설명문, 주석을 절대 포함하지 마라.
        - JSON object 외에 다른 텍스트를 출력하지 마라.

        ## 출력 JSON schema
        {
          "keywords": ["string"],
          "typeFilters": ["TEXT"|"LINK"|"IMAGE"|"FILE"|"SCREENSHOT"],
          "dateRangeDays": null 또는 숫자,
          "maxResults": 숫자
        }

        ## 필드 규칙
        - keywords: 한국어 조사(은/는/이/가/을/를 등)는 제거한 핵심 명사/동사 중심 키워드 1~8개.
          필요하면 영어 동의어를 함께 포함해라. 빈 문자열 금지. 중복 금지.
        - typeFilters: 다음 중 검색 대상 타입. 없으면 빈 배열.
          TEXT(메모/글/텍스트), LINK(링크/URL/주소/웹사이트),
          IMAGE(사진/이미지), FILE(파일/문서/PDF/첨부), SCREENSHOT(스크린샷/캡처)
        - dateRangeDays: 시간 조건이 있으면 일수(1~365). 없으면 null.
          오늘→1, 어제→2, 이번 주→7, 지난주→14, 이번 달→30, 지난달→60, 최근→30
        - maxResults: 기본값 20. 5~50 사이.

        ## 예시
        주제: "오늘 수집한 링크와 메모 정리"
        응답: {"keywords":["링크","메모","정리","link","memo"],"typeFilters":["LINK","TEXT"],"dateRangeDays":1,"maxResults":20}

        주제: "이번 주 업무 관련 자료"
        응답: {"keywords":["업무","회의","일정","보고서","work","meeting"],"typeFilters":[],"dateRangeDays":7,"maxResults":30}

        ## 사용자 주제
        $topicQuery
    """.trimIndent()

    fun parseRetrievalPlan(raw: String): Result<RetrievalPlan> = runCatching {
        val jsonText = extractJsonObject(raw) ?: throw IllegalArgumentException("유효한 JSON을 찾을 수 없습니다.")
        val obj = json.parseToJsonElement(jsonText).jsonObject

        // 배열 파싱 로직 압축 (널 안전성과 체이닝 활용)
        val keywords = (obj["keywords"] as? JsonArray)
            ?.mapNotNull { it.jsonPrimitive.content.takeIf { c -> c != "null" && c.isNotBlank() }?.trim() }
            ?.distinct()?.take(8) ?: emptyList()

        val typeFilters = (obj["typeFilters"] as? JsonArray)
            ?.mapNotNull { runCatching { DataItemType.valueOf(it.jsonPrimitive.content.trim().uppercase()) }.getOrNull() }
            ?.distinct() ?: emptyList()

        // 숫자 파싱 로직 압축 (toFloatOrNull을 사용해 Int/Double 포맷 모두 대응)
        val dateRangeDays = obj["dateRangeDays"]?.jsonPrimitive?.content?.takeIf { it != "null" }
            ?.toFloatOrNull()?.toInt()?.takeIf { it > 0 }?.coerceIn(1, 365)

        val maxResults = obj["maxResults"]?.jsonPrimitive?.content?.takeIf { it != "null" }
            ?.toFloatOrNull()?.toInt()?.coerceIn(5, 50) ?: 20

        RetrievalPlan(keywords, typeFilters, dateRangeDays, maxResults)
    }
}