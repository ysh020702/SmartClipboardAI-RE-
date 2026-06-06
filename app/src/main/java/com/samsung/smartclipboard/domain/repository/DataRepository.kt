package com.samsung.smartclipboard.domain.repository

import com.samsung.smartclipboard.domain.model.DataItem
import com.samsung.smartclipboard.domain.model.TopicAction
import com.samsung.smartclipboard.domain.model.Topic
import com.samsung.smartclipboard.domain.model.TopicAnalysis
import kotlinx.coroutines.flow.Flow

interface DataRepository {
    fun observeItems(): Flow<List<DataItem>>
    fun observeTopics(): Flow<List<Topic>>
    fun observeTopicItems(topicId: Long): Flow<List<DataItem>>
    fun observeTopicAnalysis(topicId: Long): Flow<List<TopicAnalysis>>
    fun observeTopicActions(topicId: Long): Flow<List<TopicAction>>
    suspend fun addText(text: String, source: String? = null)
    suspend fun addLink(url: String, title: String? = null, source: String? = null)
    suspend fun addMedia(uri: String, mimeType: String? = null, source: String? = null)
    suspend fun addScreenshot(
        uri: String,
        title: String? = null,
        mimeType: String? = null,
        source: String? = null,
        createdAt: Long = System.currentTimeMillis()
    )
    suspend fun updateScreenshotTimestamp(uri: String, createdAt: Long)
    suspend fun deleteItem(id: Long)
    suspend fun clearAll()
    suspend fun addItemsToTopic(title: String, itemIds: List<Long>, addedBy: String = "USER"): Long
    suspend fun runTopicAnalysis(topicId: Long): Boolean
    suspend fun updateTopicActionDraft(actionId: Long, title: String, body: String)

    /** purpose가 없는 DataItem들에 대해 Gemini 기반 purpose 분석을 수행하고 DB를 업데이트한다 */
    suspend fun fillPurposes()

    /** 지정된 ID 목록에 해당하는 DataItem들을 반환한다 */
    suspend fun getItemsByIds(ids: List<Long>): List<DataItem>
}
