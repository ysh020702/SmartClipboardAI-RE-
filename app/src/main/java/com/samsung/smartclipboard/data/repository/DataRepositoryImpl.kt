package com.samsung.smartclipboard.data.repository

import android.util.Log
import com.samsung.smartclipboard.data.ai.DefaultSourceExtractor
import com.samsung.smartclipboard.gemini.GeminiTaskAgent
import com.samsung.smartclipboard.database.dao.DataItemDao
import com.samsung.smartclipboard.database.dao.TopicDao
import com.samsung.smartclipboard.database.dao.TopicSummaryRow
import com.samsung.smartclipboard.database.entity.DataItemEntity
import com.samsung.smartclipboard.database.entity.TopicActionEntity
import com.samsung.smartclipboard.database.entity.TopicAnalysisEntity
import com.samsung.smartclipboard.database.entity.TopicEntity
import com.samsung.smartclipboard.database.entity.TopicItemCrossRefEntity
import com.samsung.smartclipboard.domain.model.DataItem
import com.samsung.smartclipboard.domain.model.DataItemType
import com.samsung.smartclipboard.domain.model.LinkMetadataCodec
import com.samsung.smartclipboard.domain.model.Topic
import com.samsung.smartclipboard.domain.model.TaskSelection
import com.samsung.smartclipboard.domain.model.TaskSelectionStatus
import com.samsung.smartclipboard.domain.model.TaskSelectionType
import com.samsung.smartclipboard.domain.model.TopicAnalysis
import com.samsung.smartclipboard.domain.repository.DataRepository
import com.samsung.smartclipboard.gemini.GeminiPurposeAgent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DataRepositoryImpl @Inject constructor(
    private val dataItemDao: DataItemDao,
    private val topicDao: TopicDao,
    private val topicAgent: GeminiTaskAgent,
    private val sourceExtractor: DefaultSourceExtractor,
    private val purposeAnalyzer: GeminiPurposeAgent
) : DataRepository {

    override fun observeItems(): Flow<List<DataItem>> {
        return dataItemDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeTopics(): Flow<List<Topic>> {
        return topicDao.observeTopicSummaries().map { rows ->
            rows.map { it.toDomain() }
        }
    }

    override fun observeTopicItems(topicId: Long): Flow<List<DataItem>> {
        return topicDao.observeItemsForTopic(topicId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeTopicAnalysis(topicId: Long): Flow<List<TopicAnalysis>> {
        return topicDao.observeAnalysisForTopic(topicId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeTopicActions(topicId: Long): Flow<List<TaskSelection>> {
        return topicDao.observeActionsForTopic(topicId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addText(text: String, source: String?) {
        val entity = DataItemEntity(
            type = DataItemType.TEXT.name,
            content = text,
            source = source,
            createdAt = System.currentTimeMillis()
        )
        val id = dataItemDao.insert(entity)
        analyzePurposeForItem(id)
    }

    override suspend fun addLink(url: String, title: String?, source: String?) {
        val entity = DataItemEntity(
            type = DataItemType.LINK.name,
            content = url,
            title = title,
            source = source,
            createdAt = System.currentTimeMillis()
        )
        val id = dataItemDao.insert(entity)

        enrichLinkMetadata(id = id, url = url, fallbackTitle = title)

        analyzePurposeForItem(id)
    }

    override suspend fun enrichLinkMetadata(itemId: Long): Boolean {
        val item = dataItemDao.getItemsByIds(listOf(itemId)).firstOrNull()?.toDomain() ?: return false
        if (item.type != DataItemType.LINK) return false
        return enrichLinkMetadata(id = item.id, url = item.content, fallbackTitle = item.title)
    }

    override suspend fun addMedia(uri: String, mimeType: String?, source: String?) {
        val type = if (mimeType?.startsWith("image/") == true) {
            DataItemType.IMAGE
        } else {
            DataItemType.FILE
        }
        val entity = DataItemEntity(
            type = type.name,
            content = uri,
            mimeType = mimeType,
            source = source,
            createdAt = System.currentTimeMillis()
        )
        val id = dataItemDao.insert(entity)

        if (type == DataItemType.IMAGE) {
            try {
                val extracted = sourceExtractor.extractFromOcr(uri)
                if (extracted.isNotBlank()) {
                    dataItemDao.updateExtractedContent(id, extracted)
                }
            } catch (e: Exception) {
                Log.w("DataRepository", "OCR 추출 실패 (media): $uri", e)
            }
        }

        analyzePurposeForItem(id)
    }

    override suspend fun addScreenshot(
        uri: String,
        title: String?,
        mimeType: String?,
        source: String?,
        createdAt: Long
    ) {
        val entity = DataItemEntity(
            type = DataItemType.SCREENSHOT.name,
            content = uri,
            title = title,
            mimeType = mimeType,
            source = source,
            createdAt = createdAt
        )
        val id = dataItemDao.insert(entity)

        try {
            val extracted = sourceExtractor.extractFromOcr(uri)
            if (extracted.isNotBlank()) {
                dataItemDao.updateExtractedContent(id, extracted)
            }
        } catch (e: Exception) {
            Log.w("DataRepository", "OCR 추출 실패 (screenshot): $uri", e)
        }

        analyzePurposeForItem(id)
    }

    override suspend fun updateScreenshotTimestamp(uri: String, createdAt: Long) {
        dataItemDao.updateCreatedAtByContentAndType(
            content = uri,
            type = DataItemType.SCREENSHOT.name,
            createdAt = createdAt
        )
    }

    override suspend fun deleteItem(id: Long) {
        dataItemDao.deleteById(id)
    }

    override suspend fun clearAll() {
        dataItemDao.clearAll()
    }

    override suspend fun addItemsToTopic(title: String, itemIds: List<Long>, addedBy: String): Long {
        val normalizedTitle = title.trim()
        require(normalizedTitle.isNotBlank()) { "Topic title must not be blank" }
        val now = System.currentTimeMillis()
        val topicId = topicDao.findTopicIdByTitleAndCreatedAt(normalizedTitle, now)
            ?: topicDao.insertTopic(
                TopicEntity(
                    title = normalizedTitle,
                    createdAt = now,
                    updatedAt = now
                )
            )

        val refs = itemIds.distinct().map { itemId ->
            TopicItemCrossRefEntity(
                topicId = topicId,
                itemId = itemId,
                addedAt = now,
                addedBy = addedBy
            )
        }
        if (refs.isNotEmpty()) {
            topicDao.insertTopicItemRefs(refs)
        }
        topicDao.updateTopicTimestamp(topicId, now)
        return topicId
    }

    override suspend fun runTopicAnalysis(topicId: Long): Boolean {
        val items = topicDao.observeItemsForTopic(topicId).first().map { it.toDomain() }
        if (items.isEmpty()) return false

        val topicRow = topicDao.getTopicById(topicId) ?: return false
        val topic = Topic(
            id = topicRow.id,
            title = topicRow.title,
            itemCount = items.size,
            createdAt = topicRow.createdAt,
            updatedAt = topicRow.updatedAt
        )

        val now = System.currentTimeMillis()

        val result = topicAgent.analyze(topic, items,"")

        var success = false

        result.onSuccess { agentResult ->
            val analysisId = topicDao.insertAnalysis(
                TopicAnalysisEntity(
                    topicId = topicId,
                    summary = agentResult.summary,
                    keyPoints = agentResult.keyPoints.joinToString("\n"),
                    sourceItemIds = agentResult.sourceItemIds.joinToString(","),
                    createdAt = now
                )
            )

            val actionEntities = agentResult.actions.map { draft ->
                TopicActionEntity(
                    topicId = topicId,
                    analysisResultId = analysisId,
                    type = draft.type.name,
                    title = draft.title,
                    body = draft.body,
                    status = TaskSelectionStatus.DRAFT.name,
                    editablePayload = draft.payload,
                    createdAt = now,
                    updatedAt = now
                )
            }
            if (actionEntities.isNotEmpty()) {
                topicDao.insertActions(actionEntities)
            }
            success = true
        }

        result.onFailure { exception ->
            android.util.Log.e("DataRepository", "TopicAgent 분석 실패: topicId=$topicId", exception)
        }

        topicDao.updateTopicTimestamp(topicId, now)
        return success
    }

    override suspend fun updateTopicActionDraft(actionId: Long, title: String, body: String) {
        topicDao.updateActionDraft(
            actionId = actionId,
            title = title,
            body = body,
            status = TaskSelectionStatus.EDITED.name,
            updatedAt = System.currentTimeMillis()
        )
    }

    override suspend fun updateActionStatus(actionId: Long, status: TaskSelectionStatus) {
        topicDao.updateActionStatus(
            actionId = actionId,
            status = status.name,
            updatedAt = System.currentTimeMillis()
        )
    }

    override suspend fun fillPurposes() {
        val entities = dataItemDao.getItemsWithoutPurpose()
        if (entities.isEmpty()) return

        val items = entities.map { it.toDomain() }
        val result = purposeAnalyzer.analyze(items)

        result.onSuccess { analyzedList ->
            for (analyzed in analyzedList) {
                try {
                    dataItemDao.updatePurpose(analyzed.itemId, analyzed.purpose, analyzed.purposeKeyword)
                } catch (e: Exception) {
                    Log.w("DataRepository", "Purpose 업데이트 실패: itemId=${analyzed.itemId}", e)
                }
            }
        }

        result.onFailure { exception ->
            Log.e("DataRepository", "Purpose 분석 전체 실패", exception)
        }
    }

    override suspend fun getItemsByIds(ids: List<Long>): List<DataItem> {
        if (ids.isEmpty()) return emptyList()
        return dataItemDao.getItemsByIds(ids).map { it.toDomain() }
    }

    override fun observeAllTopicActions(): Flow<List<TaskSelection>> {
        return topicDao.observeAllActions().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeAllTopicAnalysis(): Flow<List<TopicAnalysis>> {
        return topicDao.observeAllAnalysis().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getActionById(actionId: Long): TaskSelection? {
        return topicDao.getActionById(actionId)?.toDomain()
    }

    override suspend fun deleteTopicById(topicId: Long) {
        topicDao.deleteActionsByTopicId(topicId)
        topicDao.deleteAnalysisByTopicId(topicId)
        topicDao.deleteCrossRefsByTopicId(topicId)
        topicDao.deleteTopicById(topicId)
    }

    override suspend fun deleteTopicsByIds(topicIds: List<Long>) {
        topicIds.forEach { id -> deleteTopicById(id) }
    }

    override fun observeItemsInPeriod(startMs: Long?, endMs: Long?): Flow<List<DataItem>> {
        val flow = when {
            startMs != null && endMs != null -> dataItemDao.observeAllInRange(startMs, endMs)
            startMs != null -> dataItemDao.observeAllFromStart(startMs)
            endMs != null -> dataItemDao.observeAllUntilEnd(endMs)
            else -> dataItemDao.observeAll()
        }
        return flow.map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getItemCount(startMs: Long?, endMs: Long?): Int {
        return when {
            startMs != null && endMs != null -> dataItemDao.countInRange(startMs, endMs)
            startMs != null -> dataItemDao.countFromStart(startMs)
            endMs != null -> dataItemDao.countUntilEnd(endMs)
            else -> dataItemDao.countAll()
        }
    }

    /**
     * 단일 아이템에 대해 purpose 분석을 수행하고 DB를 업데이트한다.
     * 실패해도 아이템 저장 자체에는 영향이 없도록 예외를 삼킨다.
     */
    private suspend fun analyzePurposeForItem(id: Long) {
        try {
            val entity = dataItemDao.getItemsWithoutPurpose().find { it.id == id } ?: return
            val item = entity.toDomain()
            val result = purposeAnalyzer.analyze(listOf(item))
            result.onSuccess { analyzedList ->
                val analyzed = analyzedList.firstOrNull() ?: return@onSuccess
                dataItemDao.updatePurpose(analyzed.itemId, analyzed.purpose, analyzed.purposeKeyword)
                Log.d("aaa",analyzed.purpose)
                Log.d("aaa",analyzed.purposeKeyword)
            }
            result.onFailure { e ->
                Log.w("DataRepository", "단일 항목 purpose 분석 실패: id=$id", e)
            }
        } catch (e: Exception) {
            Log.w("DataRepository", "Purpose 분석 중 예외: id=$id", e)
        }
    }

    private suspend fun enrichLinkMetadata(id: Long, url: String, fallbackTitle: String?): Boolean {
        return try {
            val metadata = sourceExtractor.extractFromUrl(url)
            val encoded = LinkMetadataCodec.encode(metadata) ?: return false
            dataItemDao.updateLinkMetadata(
                id = id,
                title = metadata.title ?: fallbackTitle,
                extractedContent = encoded,
            )
            true
        } catch (e: Exception) {
            Log.w("DataRepository", "URL 메타데이터 추출 실패: $url", e)
            false
        }
    }

    private fun DataItemEntity.toDomain(): DataItem {
        val resolvedType = try {
            DataItemType.valueOf(type)
        } catch (e: IllegalArgumentException) {
            DataItemType.TEXT
        }
        return DataItem(
            id = id,
            type = resolvedType,
            content = content,
            title = title,
            source = source,
            mimeType = mimeType,
            createdAt = createdAt,
            extractedContent = extractedContent,
            purpose = purpose,
            purposeKeyword = purposeKeyword
        )
    }

    private fun TopicSummaryRow.toDomain(): Topic {
        return Topic(
            id = id,
            title = title,
            itemCount = itemCount,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun TopicAnalysisEntity.toDomain(): TopicAnalysis {
        return TopicAnalysis(
            id = id,
            topicId = topicId,
            summary = summary,
            keyPoints = keyPoints.lines().filter { it.isNotBlank() },
            sourceItemIds = sourceItemIds.split(",").mapNotNull { it.trim().toLongOrNull() },
            createdAt = createdAt
        )
    }

    private fun TopicActionEntity.toDomain(): TaskSelection {
        return TaskSelection(
            id = id,
            topicId = topicId,
            analysisResultId = analysisResultId,
            type = runCatching { TaskSelectionType.valueOf(type) }.getOrDefault(TaskSelectionType.SUMMARY),
            title = title,
            body = body,
            status = runCatching { TaskSelectionStatus.valueOf(status) }.getOrDefault(TaskSelectionStatus.DRAFT),
            editablePayload = editablePayload,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

}
