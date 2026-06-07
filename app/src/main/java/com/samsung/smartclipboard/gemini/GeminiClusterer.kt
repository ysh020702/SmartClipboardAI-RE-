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
            Log.w("aaa",buildPrompt(limited))
            val raw = geminiManager.run(buildPrompt(limited))
            Log.w("aaa",raw)

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
            아래 데이터 항목들을 분석하여 의미와 목적이 통하는 것끼리 그룹으로 묶어주세요.
    
            ## 클러스터링 핵심 기준 (매우 중요)
            1. '검색', '쇼핑', '준비', '예약'과 같은 지나치게 포괄적이고 추상적인 '행동(Action)' 위주로 묶지 마라.
            2. 대신, 데이터들이 공통으로 공유하는 **구체적인 핵심 대상(Target, Entity, 브랜드, 프로젝트 등)**을 찾아 그룹화하라.
            3. 세부 내용이 조금 다르더라도 **동일한 상위 타겟/맥락(예: 같은 목적지의 장소들, 같은 브랜드의 제품들, 같은 프로젝트의 문서들)**에 속한다면 하나의 클러스터로 묶어라.
            4. 완전히 무관한 대상(예: 서로 다른 지역, 전혀 다른 브랜드/도메인)일 경우에만 다른 클러스터로 분리하라.
    
            ## 일반 규칙
            - 응답은 반드시 JSON object 하나만 출력한다.
            - markdown 코드 펜스, 설명문, 주석을 절대 포함하지 마라.
            - 각 데이터 항목의 purpose와 keywords를 종합적으로 고려한다.
            - 클러스터 ID는 1부터 시작하는 정수를 사용한다.
            - 클러스터 개수는 최대 50개까지 가능하며, 단일 항목 클러스터도 허용한다.
            - 각 클러스터에 대해 공통 대상을 나타내는 간결한 한국어 라벨(15자 이내)을 작성한다.
    
            ## 출력 JSON schema
            {
              "assignments": [1, 2, 1, 3, ...],
              "clusters": {
                "1": "클러스터 라벨 1",
                "2": "클러스터 라벨 2",
                "3": "클러스터 라벨 3",
                ...
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
                "purpose": ${item.purpose?.let { "\"${escapeJson(it)}\"" }}
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