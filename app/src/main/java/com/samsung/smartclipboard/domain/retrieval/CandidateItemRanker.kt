package com.samsung.smartclipboard.domain.retrieval

import com.samsung.smartclipboard.domain.model.CandidateItem
import com.samsung.smartclipboard.domain.model.DataItem
import com.samsung.smartclipboard.domain.model.RetrievalPlan

/**
 * 검색된 DataItem 목록에 점수를 부여하고 CandidateItem 목록으로 변환하는 인터페이스.
 *
 * LLM 호출 없이 순수 Kotlin 점수 계산만 수행한다.
 */
interface CandidateItemRanker {

    /**
     * @param topicQuery 사용자가 입력한 주제 문자열
     * @param plan 검색 계획
     * @param items DataRetriever가 검색한 DataItem 목록
     * @return relevanceScore 기준 내림차순 정렬된 CandidateItem 목록
     */
    fun rank(
        topicQuery: String,
        plan: RetrievalPlan,
        items: List<DataItem>
    ): List<CandidateItem>
}