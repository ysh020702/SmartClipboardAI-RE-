package com.samsung.smartclipboard.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.samsung.smartclipboard.database.entity.DataItemEntity
import com.samsung.smartclipboard.database.entity.TopicActionEntity
import com.samsung.smartclipboard.database.entity.TopicAnalysisEntity
import com.samsung.smartclipboard.database.entity.TopicEntity
import com.samsung.smartclipboard.database.entity.TopicItemCrossRefEntity
import kotlinx.coroutines.flow.Flow

data class TopicSummaryRow(
    val id: Long,
    val title: String,
    val itemCount: Int,
    val createdAt: Long,
    val updatedAt: Long
)

@Dao
interface TopicDao {
    @Query(
        """
        SELECT
            topics.id AS id,
            topics.title AS title,
            COUNT(topic_item_cross_refs.itemId) AS itemCount,
            topics.createdAt AS createdAt,
            topics.updatedAt AS updatedAt
        FROM topics
        LEFT JOIN topic_item_cross_refs ON topics.id = topic_item_cross_refs.topicId
        GROUP BY topics.id
        ORDER BY topics.updatedAt DESC
        """
    )
    fun observeTopicSummaries(): Flow<List<TopicSummaryRow>>

    @Query("SELECT id FROM topics WHERE title = :title COLLATE NOCASE AND createdAt = :createdAt LIMIT 1")
    suspend fun findTopicIdByTitleAndCreatedAt(title: String, createdAt: Long): Long?

    @Query(
        """
        SELECT
            topics.id AS id,
            topics.title AS title,
            COUNT(topic_item_cross_refs.itemId) AS itemCount,
            topics.createdAt AS createdAt,
            topics.updatedAt AS updatedAt
        FROM topics
        LEFT JOIN topic_item_cross_refs ON topics.id = topic_item_cross_refs.topicId
        WHERE topics.id = :topicId
        GROUP BY topics.id
        """
    )
    suspend fun getTopicById(topicId: Long): TopicSummaryRow?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTopic(topic: TopicEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTopicItemRefs(refs: List<TopicItemCrossRefEntity>)

    @Query("UPDATE topics SET updatedAt = :updatedAt WHERE id = :topicId")
    suspend fun updateTopicTimestamp(topicId: Long, updatedAt: Long)

    @Query("SELECT * FROM data_items WHERE id IN (SELECT itemId FROM topic_item_cross_refs WHERE topicId = :topicId) ORDER BY createdAt DESC")
    fun observeItemsForTopic(topicId: Long): Flow<List<DataItemEntity>>

    @Query("SELECT * FROM topic_analysis_results WHERE topicId = :topicId ORDER BY createdAt DESC")
    fun observeAnalysisForTopic(topicId: Long): Flow<List<TopicAnalysisEntity>>

    @Query("SELECT * FROM topic_actions WHERE topicId = :topicId ORDER BY updatedAt DESC")
    fun observeActionsForTopic(topicId: Long): Flow<List<TopicActionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysis(result: TopicAnalysisEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActions(actions: List<TopicActionEntity>)

    @Query(
        """
        UPDATE topic_actions
        SET title = :title,
            body = :body,
            status = :status,
            updatedAt = :updatedAt
        WHERE id = :actionId
        """
    )
    suspend fun updateActionDraft(
        actionId: Long,
        title: String,
        body: String,
        status: String,
        updatedAt: Long
    )

    @Query(
        """
        UPDATE topic_actions
        SET status = :status,
            updatedAt = :updatedAt
        WHERE id = :actionId
        """
    )
    suspend fun updateActionStatus(
        actionId: Long,
        status: String,
        updatedAt: Long
    )

    @Query("SELECT * FROM topic_actions ORDER BY topicId, updatedAt DESC")
    fun observeAllActions(): Flow<List<TopicActionEntity>>

    @Query("SELECT * FROM topic_analysis_results ORDER BY topicId, createdAt DESC")
    fun observeAllAnalysis(): Flow<List<TopicAnalysisEntity>>

    @Query("SELECT * FROM topic_actions WHERE id = :actionId LIMIT 1")
    suspend fun getActionById(actionId: Long): TopicActionEntity?

    @Query("DELETE FROM topic_actions WHERE topicId = :topicId")
    suspend fun deleteActionsByTopicId(topicId: Long)

    @Query("DELETE FROM topic_analysis_results WHERE topicId = :topicId")
    suspend fun deleteAnalysisByTopicId(topicId: Long)

    @Query("DELETE FROM topic_item_cross_refs WHERE topicId = :topicId")
    suspend fun deleteCrossRefsByTopicId(topicId: Long)

    @Query("DELETE FROM topics WHERE id = :topicId")
    suspend fun deleteTopicById(topicId: Long)

    @Query("DELETE FROM topic_actions WHERE id IN (:actionIds)")
    suspend fun deleteActionsByIds(actionIds: List<Long>)
}
