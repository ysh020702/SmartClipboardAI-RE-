package com.samsung.smartclipboard.gemini

import android.util.Log
import com.samsung.smartclipboard.data.retrieval.LocalClusterer
import com.samsung.smartclipboard.domain.model.DataCluster
import com.samsung.smartclipboard.domain.model.DataItem
import com.samsung.smartclipboard.domain.retrieval.DataClusterer
import com.samsung.smartclipboard.gemini.GeminiUtils.contentPreview
import com.samsung.smartclipboard.gemini.GeminiUtils.escapeJson
import com.samsung.smartclipboard.gemini.GeminiUtils.extractJsonObject
import org.json.JSONObject

class GeminiClusterer(
    private val geminiManager: GeminiManager,
    private val fallback: LocalClusterer
) : DataClusterer {

    companion object {
        private const val TAG = "GeminiClusterer"
        private const val MAX_ITEMS = 50
    }

    override suspend fun cluster(items: List<DataItem>): List<DataCluster> {
        val hasPurpose = items.any { !it.purpose.isNullOrBlank() || !it.purposeKeyword.isNullOrBlank() }
        if (items.isEmpty() || !hasPurpose) {
            if (!hasPurpose) Log.w(TAG, "purpose가 있는 아이템이 없어 로컬 클러스터링으로 폴백")
            return fallback.cluster(items)
        }

        val limited = items.take(MAX_ITEMS)
        val remaining = items.drop(MAX_ITEMS)

        return try {
            val raw = geminiManager.run(buildPrompt(limited))

            // 파싱 결과가 null이거나, assignments 크기가 맞지 않으면 null 반환 (폴백 유도)
            val result = parse(raw)?.takeIf { it.assignments.size == limited.size }

            if (result == null) {
                Log.w(TAG, "Gemini 파싱 실패 또는 크기 불일치, 로컬 폴백")
                return fallback.cluster(items)
            }

            // limited와 assignments를 1:1로 묶은 뒤(zip), clusterId를 기준으로 그룹화(groupBy)
            val clusters = limited.zip(result.assignments)
                .groupBy({ it.second }, { it.first })
                .map { (clusterId, groupItems) ->
                    val label = result.clusterLabels[clusterId]
                        ?: groupItems.firstOrNull()?.purpose?.take(15)
                        ?: "데이터 묶음 $clusterId"

                    DataCluster(
                        clusterId = "cluster_$clusterId",
                        clusterLabel = label.take(40),
                        itemIds = groupItems.map { it.id },
                        topicCandidates = emptyList(),
                        generatedAt = System.currentTimeMillis()
                    )
                }.sortedByDescending { it.itemIds.size }

            val extraClusters = if (remaining.isNotEmpty()) fallback.cluster(remaining) else emptyList()
            clusters + extraClusters

        } catch (e: Exception) {
            Log.e(TAG, "Gemini 클러스터링 실패, 로컬 폴백", e)
            fallback.cluster(items)
        }
    }

    private fun buildPrompt(items: List<DataItem>): String {
        return """
            당신은 데이터 클러스터링 전문가입니다.
            아래 데이터 항목들을 의미적으로 관련된 것끼리 그룹으로 묶어주세요.

            ## 반드시 지킬 규칙
            - 응답은 반드시 JSON object 하나만 출력한다.
            - markdown 코드 펜스, 설명문, 주석을 절대 포함하지 마라.
            - 각 데이터 항목의 purpose(검색 목적)와 keywords(핵심 키워드)를 중심으로 관련 항목끼리 묶어라.
            - 클러스터 ID는 1부터 시작하는 정수를 사용한다.
            - 관련 없는 항목은 서로 다른 클러스터에 배정한다.
            - 클러스터 개수는 최대 10개까지 가능하며, 단일 항목만 있는 클러스터도 허용한다.
            - 각 클러스터에 대해 간결한 한국어 라벨(15자 이내)을 작성한다.

            ## 출력 JSON schema
            {
              "assignments": [1, 2, 1],
              "clusters": {
                "1": "클러스터 라벨 1",
                "2": "클러스터 라벨 2"
              }
            }

            ## 필드 규칙
            - assignments: 입력 데이터와 동일한 순서로 각 항목의 클러스터 ID를 나열한 배열
            - clusters: 클러스터 ID(문자열)를 키로, 라벨을 값으로 하는 객체

            ## 입력 데이터 (${items.size}개)
            ${buildItemsString(items)}
        """.trimIndent()
    }

    private fun buildItemsString(items: List<DataItem>): String {
        return items.joinToString(separator = ",\n", prefix = "[\n", postfix = "\n]") { item ->
            """
              {
                "id": ${item.id},
                "type": "${item.type.name}",
                "title": ${item.title?.let { "\"${escapeJson(it)}\"" }},
                "purpose": ${item.purpose?.let { "\"${escapeJson(it)}\"" }},
                "keywords": ${item.purposeKeyword?.let { "\"${escapeJson(it)}\"" }},
                "contentPreview": "${escapeJson(contentPreview(item, 150))}"
              }
            """.trimIndent()
        }
    }

    data class ClusterResult(
        val assignments: List<Int>,
        val clusterLabels: Map<Int, String>
    )

    private fun parse(raw: String): ClusterResult? {
        return try {
            val jsonText = extractJsonObject(raw) ?: return null
            val json = JSONObject(jsonText)

            val assignmentsArray = json.getJSONArray("assignments")
            val assignments = List(assignmentsArray.length()) { assignmentsArray.getInt(it) }

            val clustersObj = json.getJSONObject("clusters")
            val clusterLabels = clustersObj.keys().asSequence().mapNotNull { key ->
                key.toIntOrNull()?.let { it to clustersObj.getString(key) }
            }.toMap()

            if (assignments.isEmpty()) null else ClusterResult(assignments, clusterLabels)
        } catch (e: Exception) {
            null
        }
    }
}