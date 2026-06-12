package com.samsung.smartclipboard.domain.repository

import com.samsung.smartclipboard.domain.model.DataItem
import com.samsung.smartclipboard.domain.model.TaskSelection
import com.samsung.smartclipboard.domain.model.TaskSelectionStatus
import com.samsung.smartclipboard.domain.model.Topic
import com.samsung.smartclipboard.domain.model.TopicAnalysis
import kotlinx.coroutines.flow.Flow

interface DataRepository {
    /** 모든 DataItem을 Flow로 관찰하여 반환한다 */
    fun observeItems(): Flow<List<DataItem>>

    /** 모든 Topic(요약 정보)을 Flow로 관찰하여 반환한다 */
    fun observeTopics(): Flow<List<Topic>>

    /** 특정 Topic에 속한 DataItem들을 Flow로 관찰하여 반환한다 */
    fun observeTopicItems(topicId: Long): Flow<List<DataItem>>

    /** 특정 Topic의 분석 결과들을 Flow로 관찰하여 반환한다 */
    fun observeTopicAnalysis(topicId: Long): Flow<List<TopicAnalysis>>

    /** 특정 Topic의 액션 초안들을 Flow로 관찰하여 반환한다 */
    fun observeTopicActions(topicId: Long): Flow<List<TaskSelection>>

    /** 텍스트 DataItem을 DB에 저장하고 purpose 분석을 수행한다 */
    suspend fun addText(text: String, source: String? = null)

    /** 링크 DataItem을 DB에 저장하고, URL 내용 추출 및 purpose 분석을 수행한다 */
    suspend fun addLink(url: String, title: String? = null, source: String? = null)

    /** 기존 링크 DataItem의 OG 메타데이터를 보강한다 */
    suspend fun enrichLinkMetadata(itemId: Long): Boolean

    /** 이미지/파일 DataItem을 DB에 저장하고, 이미지인 경우 OCR 추출 및 purpose 분석을 수행한다 */
    suspend fun addMedia(uri: String, mimeType: String? = null, source: String? = null)

    /** 스크린샷 DataItem을 DB에 저장하고, OCR 추출 및 purpose 분석을 수행한다 */
    suspend fun addScreenshot(
        uri: String,
        title: String? = null,
        mimeType: String? = null,
        source: String? = null,
        createdAt: Long = System.currentTimeMillis()
    )

    /** 스크린샷 아이템의 생성 시간을 지정된 값으로 갱신한다 */
    suspend fun updateScreenshotTimestamp(uri: String, createdAt: Long)

    /** 지정된 ID의 DataItem을 DB에서 삭제한다 */
    suspend fun deleteItem(id: Long)

    /** 모든 DataItem을 DB에서 삭제한다 */
    suspend fun clearAll()

    /** 아이템들을 Topic에 추가하고, Topic이 없으면 새로 생성하여 Topic ID를 반환한다 */
    suspend fun addItemsToTopic(title: String, itemIds: List<Long>, addedBy: String = "USER"): Long

    /** Topic에 속한 아이템들을 Gemini로 분석하고, 분석 결과와 액션 초안을 DB에 저장한다 */
    suspend fun runTopicAnalysis(topicId: Long): Boolean

    /** TopicAction 초안의 제목과 본문을 수정하고 상태를 EDITED로 변경한다 */
    suspend fun updateTopicActionDraft(actionId: Long, title: String, body: String)

    /** purpose가 없는 DataItem들에 대해 Gemini 기반 purpose 분석을 수행하고 DB를 업데이트한다 */
    suspend fun fillPurposes()

    /** 지정된 ID 목록에 해당하는 DataItem들을 반환한다 */
    suspend fun getItemsByIds(ids: List<Long>): List<DataItem>

    /** TopicAction의 상태를 지정된 status로 업데이트한다 */
    suspend fun updateActionStatus(actionId: Long, status: TaskSelectionStatus)

    /** 모든 토픽의 액션을 Flow로 관찰하여 반환한다 (히스토리 화면용) */
    fun observeAllTopicActions(): Flow<List<TaskSelection>>

    /** 모든 토픽의 분석 결과를 Flow로 관찰하여 반환한다 (히스토리 화면용) */
    fun observeAllTopicAnalysis(): Flow<List<TopicAnalysis>>

    /** 지정된 ID의 TopicAction을 반환한다 */
    suspend fun getActionById(actionId: Long): TaskSelection?

    /** 지정된 ID의 토픽과 관련된 모든 데이터(액션, 분석, cross-ref)를 삭제한다 */
    suspend fun deleteTopicById(topicId: Long)

    /** 여러 토픽을 한 번에 삭제한다 */
    suspend fun deleteTopicsByIds(topicIds: List<Long>)

    /** 지정된 기간 내의 DataItem을 Flow로 관찰하여 반환한다. startMs가 null이면 처음부터, endMs가 null이면 현재까지 */
    fun observeItemsInPeriod(startMs: Long?, endMs: Long?): Flow<List<DataItem>>

    /** 지정된 기간 내의 DataItem 개수를 반환한다. startMs가 null이면 처음부터, endMs가 null이면 현재까지 */
    suspend fun getItemCount(startMs: Long?, endMs: Long?): Int
}
