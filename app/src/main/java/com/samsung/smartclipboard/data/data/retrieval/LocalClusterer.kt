package com.samsung.smartclipboard.data.retrieval

import com.samsung.smartclipboard.domain.model.DataCluster
import com.samsung.smartclipboard.domain.model.DataItem
import android.util.Log
import com.samsung.smartclipboard.domain.retrieval.DataClusterer

/**
 * Jaccard 유사도 + Union-Find 기반의 로컬 클러스터링 구현체.
 *
 * 최대 300개 아이템을 대상으로 O(N^2) 유사도 계산을 수행하며,
 * threshold 이상인 아이템들을 같은 클러스터로 묶는다.
 */
class LocalClusterer : DataClusterer {

    companion object {
        private const val MAX_ITEMS = 300
        private const val SIMILARITY_THRESHOLD = 0.4f
        private const val MAX_CLUSTERS = 10
        private const val MAX_TOKENS_PER_ITEM = 50

        private val STOP_WORDS = setOf(
            "그리고", "또는", "관련", "내용", "정보", "오늘", "이번", "대한", "있는", "없는",
            "the", "and", "or", "for", "with", "from", "this", "that", "about",
            "정리", "데이터", "수집", "자료", "기록", "합니다", "있습니다", "있는지"
        )
    }

    override suspend fun cluster(items: List<DataItem>): List<DataCluster> {
        Log.d("aaa","clustering start")
        if (items.isEmpty()) return emptyList()

        // 최근순 최대 300개
        val limited = items
            .sortedByDescending { it.createdAt }
            .take(MAX_ITEMS)

        // 토큰화 (purposeKeyword가 있으면 추가 토큰으로 포함)
        val inputs = limited.map { item ->
            val textTokens = tokenize(buildSearchText(item))
            val purposeTokens = tokenizePurposeKeyword(item.purposeKeyword)
            var mergedTokens = purposeTokens
            ClusterInput(item.id, item.type.name, item.source.orEmpty(), mergedTokens, item.createdAt)
        }

        if (inputs.isEmpty()) return emptyList()

        // Union-Find
        val uf = UnionFind(inputs.size)
        for (i in inputs.indices) {
            for (j in i + 1 until inputs.size) {
                val sim = similarity(inputs[i], inputs[j])
                if (sim >= SIMILARITY_THRESHOLD) {
                    uf.union(i, j)
                }
            }
        }

        // 클러스터 그룹화
        val groups = mutableMapOf<Int, MutableList<ClusterInput>>()
        for (i in inputs.indices) {
            val root = uf.find(i)
            groups.getOrPut(root) { mutableListOf() }.add(inputs[i])
        }

        // size >= 2 클러스터 우선
        val clusters = mutableListOf<DataCluster>()
        val multiItemGroups = groups.values.filter { it.size >= 2 }
            .sortedByDescending { it.size }

        var index = 0
        for (group in multiItemGroups.take(MAX_CLUSTERS)) {
            val itemIds = group.map { it.itemId }
            val label = buildClusterLabel(group)
            clusters.add(
                DataCluster(
                    clusterId = "cluster_${index}_${itemIds.first()}",
                    clusterLabel = label,
                    itemIds = itemIds,
                    topicCandidates = emptyList(),
                    generatedAt = System.currentTimeMillis()
                )
            )
            index++
        }

        // size >= 2 클러스터가 없으면 singleton 클러스터 추가
        if (clusters.isEmpty()) {
            val singletons = limited.take(5).map { item ->
                DataCluster(
                    clusterId = "cluster_${index}_${item.id}",
                    clusterLabel = capLabel(item.title ?: "${item.type.name} 데이터"),
                    itemIds = listOf(item.id),
                    topicCandidates = emptyList(),
                    generatedAt = System.currentTimeMillis()
                )
            }
            clusters.addAll(singletons)
        }

        return clusters.take(MAX_CLUSTERS)
    }

    // --- private helpers ---

    private fun buildSearchText(item: DataItem): String {
        return listOfNotNull(
            item.title,
            item.effectiveContent.take(1000),
            item.source,
            item.mimeType,
            item.type.name
        ).joinToString(" ")
    }

    private fun tokenize(text: String): Set<String> {
        return text
            .lowercase()
            .split(Regex("[\\s,/.\n\t()\\[\\]{}'\":;]+"))
            .map { it.trim() }
            .filter { it.length >= 2 && it !in STOP_WORDS }
            .toSet()
            .take(MAX_TOKENS_PER_ITEM)
            .toSet()
    }

    /**
     * purposeKeyword(콤마 구분 문자열)를 토큰 셋으로 변환.
     * purposeKeyword는 Gemini가 이미 핵심 키워드로 추출한 값이므로
     * 클러스터링 유사도에서 높은 가중치를 갖는다.
     */
    private fun tokenizePurposeKeyword(purposeKeyword: String?): Set<String> {
        if (purposeKeyword.isNullOrBlank()) return emptySet()
        return purposeKeyword
            .lowercase()
            .split(Regex("[,·]+"))
            .map { it.trim() }
            .filter { it.length >= 2 && it !in STOP_WORDS }
            .toSet()
    }

    private fun similarity(a: ClusterInput, b: ClusterInput): Float {
        val aTokens = a.tokens
        val bTokens = b.tokens
        if (aTokens.isEmpty() || bTokens.isEmpty()) return 0.0f

        val intersection = aTokens.intersect(bTokens).size.toFloat()
        val union = aTokens.union(bTokens).size.toFloat()
        var score = if (union > 0) intersection / union else 0.0f

        // type boost
        if (a.type == b.type) score += 0.05f
        // source boost
        if (a.source == b.source && a.source.isNotBlank()) score += 0.05f

        return score.coerceIn(0.0f, 1.0f)
    }

    private fun buildClusterLabel(group: List<ClusterInput>): String {
        // 모든 아이템의 token frequency
        val freq = mutableMapOf<String, Int>()
        for (input in group) {
            for (token in input.tokens) {
                freq[token] = (freq[token] ?: 0) + 1
            }
        }
        val topTokens = freq.entries
            .sortedByDescending { it.value }
            .take(4)
            .map { it.key }

        return if (topTokens.isEmpty()) {
            capLabel("${group.first().type} 데이터 묶음")
        } else {
            capLabel(topTokens.joinToString(" · "))
        }
    }

    private fun capLabel(value: String): String {
        return if (value.length > 40) value.take(40) + "..." else value
    }

    // --- inner classes ---

    private data class ClusterInput(
        val itemId: Long,
        val type: String,
        val source: String,
        val tokens: Set<String>,
        val createdAt: Long
    )

    private class UnionFind(private val n: Int) {
        private val parent = IntArray(n) { it }
        private val rank = IntArray(n)

        fun find(x: Int): Int {
            if (parent[x] != x) parent[x] = find(parent[x])
            return parent[x]
        }

        fun union(x: Int, y: Int) {
            val rx = find(x)
            val ry = find(y)
            if (rx == ry) return
            if (rank[rx] < rank[ry]) parent[rx] = ry
            else if (rank[rx] > rank[ry]) parent[ry] = rx
            else { parent[ry] = rx; rank[rx]++ }
        }
    }
}
