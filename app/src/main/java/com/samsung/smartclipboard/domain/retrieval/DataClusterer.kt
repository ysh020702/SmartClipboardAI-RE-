package com.samsung.smartclipboard.domain.retrieval

import com.samsung.smartclipboard.domain.model.DataCluster
import com.samsung.smartclipboard.domain.model.DataItem

/**
 * 전체 DataItem 목록을 로컬 알고리즘으로 클러스터링하는 인터페이스.
 *
 * LLM 호출 없이, Kotlin 코드만으로 Jaccard 유사도 기반 클러스터링을 수행한다.
 * 생성된 DataCluster.topicCandidates는 emptyList이며, ClusterTopicAgent가 채운다.
 */
interface DataClusterer {

    /**
     * @param items 전체 DataItem 목록
     * @return DataCluster 목록 (itemIds는 items의 id만 포함)
     */
    suspend fun cluster(items: List<DataItem>): List<DataCluster>
}