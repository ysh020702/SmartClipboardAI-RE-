package com.samsung.smartclipboard.domain.retrieval

import com.samsung.smartclipboard.domain.model.DataItem
import com.samsung.smartclipboard.domain.model.RetrievalPlan

/**
 * RetrievalPlan을 기반으로 로컬 DB에서 DataItem을 검색하는 인터페이스.
 *
 * 반환 타입은 List<DataItem>이며, 점수화/랭킹은 CandidateItemRanker가 담당한다.
 */
interface DataRetriever {

    /**
     * 주어진 검색 계획에 따라 DataItem 목록을 검색한다.
     *
     * @param plan LLM TopicPlanner가 생성한 검색 계획
     * @return 필터링/정렬된 DataItem 목록 (최대 plan.maxResults 개)
     */
    suspend fun retrieve(plan: RetrievalPlan): List<DataItem>
}