package com.samsung.smartclipboard.data.retrieval

import com.samsung.smartclipboard.domain.model.CandidateItem
import com.samsung.smartclipboard.domain.model.DataItem
import com.samsung.smartclipboard.domain.model.RetrievalPlan
import com.samsung.smartclipboard.domain.retrieval.CandidateItemRanker

/**
 * 순수 Kotlin 기반 CandidateItem 점수 부여 구현체.
 *
 * LLM을 사용하지 않고 키워드 매칭, 타입 필터, 최신성 기반으로
 * relevanceScore (0.0f ~ 1.0f)와 relevanceReason을 생성한다.
 */
class LocalCandidateItemRanker : CandidateItemRanker {

    override fun rank(
        topicQuery: String,
        plan: RetrievalPlan,
        items: List<DataItem>
    ): List<CandidateItem> {
        val allKeywords = buildAllKeywords(topicQuery, plan.keywords)

        return items
            .map { item ->
                val score = calculateScore(item, plan, allKeywords)
                val reason = buildReason(item, plan, allKeywords)
                CandidateItem(
                    item = item,
                    relevanceScore = score,
                    relevanceReason = reason
                )
            }
            .sortedWith(
                compareByDescending<CandidateItem> { it.relevanceScore }
                    .thenByDescending { it.item.createdAt }
            )
    }

    // --- private helpers ---

    private fun buildAllKeywords(topicQuery: String, planKeywords: List<String>): List<String> {
        val tokens = topicQuery
            .split(Regex("\\s+"))
            .map { it.trim() }
            .filter { it.length >= 2 }
        return (tokens + planKeywords).distinct()
    }

    private fun calculateScore(
        item: DataItem,
        plan: RetrievalPlan,
        allKeywords: List<String>
    ): Float {
        val keywordScore = if (allKeywords.isNotEmpty()) {
            val matched = matchKeywordCount(item, allKeywords)
            matched.toFloat() / allKeywords.size.toFloat()
        } else {
            0.35f
        }

        val typeBoost = if (plan.typeFilters.isNotEmpty() && item.type in plan.typeFilters) {
            0.10f
        } else {
            0.0f
        }

        val recencyBoost = if (plan.dateRangeDays != null && plan.dateRangeDays > 0) {
            val cutoff = System.currentTimeMillis() - plan.dateRangeDays * 24L * 60L * 60L * 1000L
            if (item.createdAt >= cutoff) 0.10f else 0.0f
        } else {
            0.0f
        }

        val raw = 0.15f + keywordScore * 0.65f + typeBoost + recencyBoost
        return raw.coerceIn(0.0f, 1.0f)
    }

    private fun matchKeywordCount(item: DataItem, keywords: List<String>): Int {
        val searchText = listOfNotNull(
            item.title,
            item.effectiveContent,
            item.source,
            item.mimeType
        ).joinToString(" ").lowercase().trim()

        return keywords.count { kw ->
            kw.lowercase().trim().let { k ->
                k.isNotBlank() && searchText.contains(k)
            }
        }
    }

    private fun buildReason(
        item: DataItem,
        plan: RetrievalPlan,
        allKeywords: List<String>
    ): String {
        val parts = mutableListOf<String>()
        val matched = allKeywords.filter { kw ->
            val searchText = listOfNotNull(
                item.title,
                item.effectiveContent,
                item.source,
                item.mimeType
            ).joinToString(" ").lowercase().trim()
            kw.lowercase().trim().let { k ->
                k.isNotBlank() && searchText.contains(k)
            }
        }

        if (matched.isNotEmpty()) {
            val preview = matched.take(3).joinToString(", ")
            parts.add("키워드 ${matched.size}개 일치: $preview")
        }

        if (plan.typeFilters.isNotEmpty() && item.type in plan.typeFilters) {
            parts.add("타입 필터 일치")
        }

        if (plan.dateRangeDays != null && plan.dateRangeDays > 0) {
            val cutoff = System.currentTimeMillis() - plan.dateRangeDays * 24L * 60L * 60L * 1000L
            if (item.createdAt >= cutoff) {
                parts.add("최근 ${plan.dateRangeDays}일 내 데이터")
            }
        }

        if (parts.isEmpty()) {
            parts.add("최근 수집 데이터 기준 추천")
        }

        return parts.joinToString("; ")
    }
}
