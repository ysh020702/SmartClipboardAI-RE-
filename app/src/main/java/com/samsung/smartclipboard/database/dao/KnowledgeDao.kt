package com.samsung.smartclipboard.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.samsung.smartclipboard.database.entity.KnowledgeEntity

@Dao
interface KnowledgeDao {
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insert(entity: KnowledgeEntity)

    @Query("SELECT * FROM knowledge_table ORDER BY createdAt DESC")
    suspend fun getAll(): List<KnowledgeEntity>
}