package com.samsung.smartclipboard.data.retrieval

import com.samsung.smartclipboard.domain.model.DataItem
import com.samsung.smartclipboard.domain.model.RetrievalPlan
import com.samsung.smartclipboard.domain.repository.DataRepository
import com.samsung.smartclipboard.domain.retrieval.DataRetriever
import kotlinx.coroutines.flow.first

/**
 * DataRepository.observeItems()를 통해 현재 저장된 DataItem 전체를 가져와
 * RetrievalPlan 조건에 따라 필터링/정렬/제한하는 구현체.
 *
 * DI 미등록 상태이며, M5 ViewModel에서 주입하여 사용할 예정이다.
 */
class LocalDataRetriever(
    private val dataRepository: DataRepository
) : DataRetriever {

    override suspend fun retrieve(plan: RetrievalPlan): List<DataItem> {
        val allItems = dataRepository.observeItems().first()
        val effectiveMax = safeMaxResults(plan.maxResults)

        return allItems
            .asSequence()
            .filter { item -> typeFilterPass(item, plan.typeFilters) }
            .filter { item -> dateRangePass(item, plan.dateRangeDays) }
            .filter { item -> keywordPass(item, plan.keywords) }
            .distinctBy { it.id }
            .map { item ->
                item to keywordMatchCount(item, plan.keywords)
            }
            .sortedWith(
                compareByDescending<Pair<DataItem, Int>> { it.second }
                    .thenByDescending { it.first.createdAt }
            )
            .map { it.first }
            .take(effectiveMax)
            .toList()
    }

    // --- private helpers ---

    private fun normalize(value: String): String {
        return value.lowercase().trim()
    }

    private fun buildSearchText(item: DataItem): String {
        return listOfNotNull(
            item.title,
            item.effectiveContent,
            item.source,
            item.mimeType
        ).joinToString(" ")
    }

    private fun keywordMatchCount(item: DataItem, keywords: List<String>): Int {
        if (keywords.isEmpty()) return 0
        val searchText = normalize(buildSearchText(item))
        return keywords.count { keyword ->
            normalize(keyword).let { kw ->
                kw.isNotBlank() && searchText.contains(kw)
            }
        }
    }

    private fun safeMaxResults(maxResults: Int): Int {
        return when {
            maxResults <= 0 -> 20
            maxResults > 100 -> 100
            else -> maxResults
        }
    }

    private fun typeFilterPass(item: DataItem, typeFilters: List<*>): Boolean {
        if (typeFilters.isEmpty()) return true
        return item.type in typeFilters
    }

    private fun dateRangePass(item: DataItem, dateRangeDays: Int?): Boolean {
        if (dateRangeDays == null || dateRangeDays <= 0) return true
        val cutoff = System.currentTimeMillis() - dateRangeDays * 24L * 60L * 60L * 1000L
        return item.createdAt >= cutoff
    }

    private fun keywordPass(item: DataItem, keywords: List<String>): Boolean {
        if (keywords.isEmpty()) return true
        return keywordMatchCount(item, keywords) > 0
    }
}
