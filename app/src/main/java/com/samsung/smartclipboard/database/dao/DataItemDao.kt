package com.samsung.smartclipboard.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.samsung.smartclipboard.database.entity.DataItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DataItemDao {
    @Query("SELECT * FROM data_items ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DataItemEntity>>

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insert(entity: DataItemEntity): Long

    @Query("DELETE FROM data_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE data_items SET createdAt = :createdAt WHERE content = :content AND type = :type")
    suspend fun updateCreatedAtByContentAndType(content: String, type: String, createdAt: Long)

    @Query("UPDATE data_items SET extractedContent = :extractedContent WHERE id = :id")
    suspend fun updateExtractedContent(id: Long, extractedContent: String)

    @Query("UPDATE data_items SET purpose = :purpose, purposeKeyword = :purposeKeyword WHERE id = :id")
    suspend fun updatePurpose(id: Long, purpose: String, purposeKeyword: String)

    @Query("SELECT * FROM data_items WHERE purpose IS NULL ORDER BY createdAt DESC")
    suspend fun getItemsWithoutPurpose(): List<DataItemEntity>

    @Query("SELECT * FROM data_items WHERE id IN (:ids) ORDER BY createdAt DESC")
    suspend fun getItemsByIds(ids: List<Long>): List<DataItemEntity>

    @Query("DELETE FROM data_items")
    suspend fun clearAll()
}