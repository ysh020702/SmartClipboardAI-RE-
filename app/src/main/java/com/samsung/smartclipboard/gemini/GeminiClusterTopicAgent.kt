package com.samsung.smartclipboard.gemini

import com.samsung.smartclipboard.domain.model.DataCluster
import com.samsung.smartclipboard.domain.model.DataItem
import com.samsung.smartclipboard.domain.model.SuggestedTopic
import com.samsung.smartclipboard.gemini.GeminiUtils.contentPreview
import com.samsung.smartclipboard.gemini.GeminiUtils.escapeJson
import com.samsung.smartclipboard.gemini.GeminiUtils.extractJsonObject
import com.samsung.smartclipboard.gemini.GeminiUtils.formatDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class GeminiClusterTopicAgent(
    private val geminiManager: GeminiManager
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun suggestTopics(
        clusters: List<DataCluster>,
        items: List<DataItem>
    ): Result<List<DataCluster>> {
        if (clusters.isEmpty()) return Result.success(emptyList())
        if (items.isEmpty()) return Result.failure(IllegalArgumentException("아이템이 비어 있습니다"))

        return try {
            val targetClusters = clusters.take(10)
            val rawResponse = geminiManager.run(build(targetClusters, items))
            val parsedResult = parseClusterTopics(rawResponse, targetClusters).getOrThrow()

            if (validateClusters(parsedResult, targetClusters)) {
                Result.success(parsedResult)
            } else {
                Result.failure(IllegalArgumentException("올바른 클러스터가 아닙니다"))
            }
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("해당 클러스터의 주제를 찾는데 실패했습니다"))
        }
    }

    private fun validateClusters(result: List<DataCluster>, originalClusters: List<DataCluster>): Boolean {
        val originalIds = originalClusters.map { it.clusterId }.toSet()
        return result.all { cluster ->
            cluster.clusterId in originalIds &&
                    cluster.itemIds.isNotEmpty() &&
                    cluster.topicCandidates.all { topic ->
                        topic.suggestedTitle.isNotBlank() &&
                                topic.confidence in 0.0f..1.0f &&
                                (topic.relatedClusterId == null || topic.relatedClusterId == cluster.clusterId)
                    }
        }
    }

    fun build(clusters: List<DataCluster>, items: List<DataItem>): String {
        return """
            너는 Android 앱에서 사용자 데이터를 분석하는 비서다.
            주어진 클러스터는 사용자의 수집 데이터를 자동으로 묶은 그룹이다.
            각 클러스터에 대해, 사용자가 AI 에이전트에게 시킬 만한 자연어 추천 주제를 만들어라.

            ## 반드시 지킬 규칙
            - 응답은 반드시 JSON object 하나만 출력한다.
            - markdown 코드 펜스, 설명문, 주석을 절대 포함하지 마라.
            - 새 clusterId를 생성하지 마라. 입력된 clusterId만 사용.
            - 새 itemId를 생성하지 마라.
            - 추천 주제는 사용자가 그대로 눌러서 AI 에이전트에 입력할 자연어 문장이어야 한다.
            - 한국어로 생성해라.
            - 개인정보, URL, 주소, 연락처 등 민감한 값을 불필요하게 그대로 재출력하지 마라.

            ## 출력 JSON schema
            {
              "clusters": [
                {
                  "clusterId": "cluster_0_12",
                  "clusterLabel": "여행 일정 · 항공권",
                  "topicCandidates": [
                    {
                      "suggestedTitle": "여행 일정과 항공권 정보를 정리해줘",
                      "description": "관련 캡처와 링크를 바탕으로 이동 일정을 정리합니다.",
                      "confidence": 0.82,
                      "reason": "여행 일정과 예약 정보가 같은 클러스터에 모여 있습니다."
                    }
                  ]
                }
              ]
            }

            ## 필드 규칙
            - clusters: 입력 클러스터 중 일부 또는 전체, 최대 10개
            - clusterId: 입력된 clusterId 중 하나만 사용
            - clusterLabel: 한국어 짧은 라벨 (2~20자 권장)
            - topicCandidates: 1~3개
            - suggestedTitle: 한국어 자연어 명령형, 8~60자
            - description: 1~2문장
            - confidence: 0.0~1.0
            - reason: 1문장 이유

            ## 클러스터 목록
            ${buildClustersJson(clusters, items.associateBy { it.id })}
        """.trimIndent()
    }

    private fun buildClustersJson(clusters: List<DataCluster>, itemById: Map<Long, DataItem>): String {
        return clusters.joinToString(",\n", "[\n", "\n]") { cluster ->
            val itemsJson = cluster.itemIds.take(8).mapNotNull { itemById[it] }.joinToString(",\n") { item ->
                """
                |      {
                |        "id": ${item.id},
                |        "type": "${item.type.name}",
                |        "title": ${item.title?.let { "\"${escapeJson(it)}\"" }},
                |        "source": ${item.source?.let { "\"${escapeJson(it)}\"" }},
                |        "contentPreview": "${escapeJson(contentPreview(item, 1000))}",
                |        "createdAt": "${formatDate(item.createdAt)}"
                |      }
                """.trimMargin()
            }
            """
            |  {
            |    "clusterId": "${escapeJson(cluster.clusterId)}",
            |    "clusterLabel": "${escapeJson(cluster.clusterLabel)}",
            |    "itemCount": ${cluster.itemIds.size},
            |    "items": [
            |$itemsJson
            |    ]
            |  }
            """.trimMargin()
        }
    }

    fun parseClusterTopics(raw: String, originalClusters: List<DataCluster>): Result<List<DataCluster>> {
        return runCatching {
            val jsonText = extractJsonObject(raw) ?: throw IllegalArgumentException("유효한 JSON을 찾을 수 없습니다.")
            val obj = json.parseToJsonElement(jsonText).jsonObject
            val originalById = originalClusters.associateBy { it.clusterId }

            val arr = obj["clusters"] as? JsonArray ?: throw IllegalArgumentException("clusters 배열이 없습니다")

            val result = arr.mapNotNull { parseCluster(it.jsonObject, originalById) }.take(10)
            if (result.isEmpty()) throw IllegalArgumentException("유효한 클러스터가 없습니다")

            result
        }
    }

    private fun parseCluster(obj: JsonObject, originalById: Map<String, DataCluster>): DataCluster? {
        val cid = obj["clusterId"]?.jsonPrimitive?.content?.trim() ?: return null
        val original = originalById[cid] ?: return null

        val label = obj["clusterLabel"]?.jsonPrimitive?.content?.trim()?.takeIf { it.isNotBlank() } ?: original.clusterLabel
        val topicsArr = obj["topicCandidates"] as? JsonArray

        val topics = topicsArr?.mapNotNull { parseSuggestedTopic(it.jsonObject, cid) }
            ?.distinctBy { it.suggestedTitle }
            ?.take(3)
            ?: emptyList()

        return original.copy(
            clusterLabel = label,
            topicCandidates = topics
        )
    }

    private fun parseSuggestedTopic(obj: JsonObject, clusterId: String): SuggestedTopic? {
        val title = obj["suggestedTitle"]?.jsonPrimitive?.content?.trim()?.takeIf { it.isNotBlank() } ?: return null

        val confidenceRaw = obj["confidence"]?.jsonPrimitive?.content
        val confidence = confidenceRaw?.toFloatOrNull()?.coerceIn(0.0f, 1.0f) ?: 0.5f

        return SuggestedTopic(
            suggestedTitle = title,
            description = obj["description"]?.jsonPrimitive?.content?.trim() ?: "관련 데이터를 바탕으로 정리, 요약, 후속 작업을 만들 수 있습니다.",
            confidence = confidence,
            reason = obj["reason"]?.jsonPrimitive?.content?.trim() ?: "비슷한 키워드와 데이터 유형이 함께 묶였습니다.",
            relatedClusterId = clusterId
        )
    }
}