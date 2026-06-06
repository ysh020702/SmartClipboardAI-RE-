package com.samsung.smartclipboard.data.ai

import android.util.Log
import com.samsung.smartclipboard.domain.model.AgentResult
import com.samsung.smartclipboard.domain.model.DataItem
import com.samsung.smartclipboard.domain.model.Topic
import com.samsung.smartclipboard.gemini.GeminiManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiTopicAgent @Inject constructor(
    private val geminiManager: GeminiManager,
    private val parser: AgentJsonParser
) {

    suspend fun analyze(
        topic: Topic,
        items: List<DataItem>,
        userInstruction: String?
    ): Result<AgentResult> {
        if (items.isEmpty()) {
            return Result.failure(IllegalArgumentException("분석할 자료가 없습니다."))
        }

        val prompt = buildPrompt(topic, items, userInstruction)

        return try {
            val rawResponse = geminiManager.run(prompt)
            if (rawResponse.isBlank()) {
                Log.w(TAG, "Gemini 응답이 비어 있습니다.")
                Result.failure(IllegalStateException("Gemini 응답이 비어 있습니다. API 키 또는 네트워크 연결을 확인하세요."))
            } else {
                Log.d(TAG, "Gemini raw response:\n$rawResponse")
                val result = parser.parse(topic.id, items.map { it.id }.toSet(), rawResponse)
                if (result.isFailure) {
                    Log.w(TAG, "Gemini JSON 파싱 실패", result.exceptionOrNull())
                }
                result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini 호출 실패", e)
            Result.failure(e)
        }
    }

    private fun buildPrompt(
        topic: Topic,
        items: List<DataItem>,
        userInstruction: String?
    ): String {
        val itemDescriptions = items.joinToString("\n---\n") { item ->
            buildString {
                append("id: ${item.id}\n")
                append("type: ${item.type.name}\n")
                if (!item.title.isNullOrBlank()) append("title: ${item.title}\n")
                append("content: ${item.effectiveContent.take(1000)}\n")
                if (!item.source.isNullOrBlank()) append("source: ${item.source}\n")
            }
        }

        val instructionLine = if (!userInstruction.isNullOrBlank()) {
            "\n[사용자 추가 지시]\n$userInstruction\n"
        } else ""

        return """
            당신은 SmartClipboardAI의 초안 생성 Agent입니다.
            주어진 Topic과 자료를 분석해 실행 가능한 초안을 JSON으로만 출력하세요.

            [금지]
            - 사용자 확인 없는 외부 앱 실행 금지
            - 설명 문장 금지. JSON만 출력
            - 근거 없는 장소, 참석자, 수신자를 상상하지 마세요
            - source item id는 입력에 있는 id만 사용하세요
            - action은 최대 5개까지 생성

            [Topic]
            title: ${topic.title}
            ${instructionLine}

            [자료 목록] (${items.size}개)
            $itemDescriptions

            [JSON 스키마]
            {
              "summary": "주제와 자료의 핵심 요약 (2~3문장)",
              "keyPoints": ["핵심 포인트 1", "2", "3"],
              "sourceItemIds": [1, 2, 3],
              "recommendedActions": [
                {
                  "type": "SUMMARY|CALENDAR|REMINDER|TODO|SHARE_DRAFT",
                  "confidence": 0.91,
                  "reason": "추천 이유 한 줄",
                  "title": "action 제목",
                  "body": "사용자가 검토할 초안 본문",
                  "payload": { "app": "NOTES|CALENDAR|REMINDER|SHARE", ... },
                  "sourceItemIds": [1, 2]
                }
              ]
            }

            [Action 생성 기준]
            - SUMMARY: 자료가 2개 이상이면 기본 후보 (노트 정리, 리서치, 회의록)
            - CALENDAR: 날짜/시간/장소 정보가 명확할 때만 (ISO-8601 형식)
            - REMINDER: 마감/제출/준비/연락 등 후속 행동이 있을 때
            - TODO: 해야 할 일은 보이지만 payload가 불충분할 때
            - SHARE_DRAFT: 누군가에게 전달할 메시지/메일 초안이 자연스러울 때

            [payload 규칙 (action type별)]
            SUMMARY → {"app":"NOTES","noteTitle":"...","noteBody":"...","sourceItemIds":[...],"needsUserInput":[]}
            CALENDAR → {"app":"CALENDAR","eventTitle":"...","eventDescription":"...","startTime":"2026-05-30T14:00:00+09:00","endTime":"...","location":null,"sourceItemIds":[...],"needsUserInput":[]}
            REMINDER → {"app":"REMINDER","reminderTitle":"...","reminderBody":"...","dueTime":"2026-05-31T09:00:00+09:00","sourceItemIds":[...],"needsUserInput":[]}
            TODO → {"app":"INTERNAL_TODO","tasks":[{"title":"...","description":"...","sourceItemIds":[...]}],"needsUserInput":[]}
            SHARE_DRAFT → {"app":"SHARE","shareTitle":"...","shareText":"...","sourceItemIds":[...],"needsUserInput":[]}
        """.trimIndent()
    }

    companion object {
        private const val TAG = "GeminiTopicAgent"
    }
}
