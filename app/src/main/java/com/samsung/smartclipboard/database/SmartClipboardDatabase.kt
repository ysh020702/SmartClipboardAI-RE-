package com.samsung.smartclipboard.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.samsung.smartclipboard.database.entity.KnowledgeEntity
import com.samsung.smartclipboard.database.entity.TopicActionEntity
import com.samsung.smartclipboard.database.entity.TopicAnalysisEntity
import com.samsung.smartclipboard.database.entity.TopicEntity
import com.samsung.smartclipboard.database.entity.TopicItemCrossRefEntity
import com.samsung.smartclipboard.data.source.local.KeywordConverters
import com.samsung.smartclipboard.database.dao.KnowledgeDao
import com.samsung.smartclipboard.database.dao.TopicDao
import com.samsung.smartclipboard.database.dao.DataItemDao
import com.samsung.smartclipboard.database.entity.DataItemEntity

@Database(
    entities = [
        DataItemEntity::class,
        TopicEntity::class,
        TopicItemCrossRefEntity::class,
        TopicAnalysisEntity::class,
        TopicActionEntity::class,
        KnowledgeEntity::class
    ],
    version = 9,
    exportSchema = false
)
@TypeConverters(KeywordConverters::class)
abstract class SmartClipboardDatabase : RoomDatabase() {
    abstract fun dataItemDao(): DataItemDao
    abstract fun topicDao(): TopicDao
    abstract fun knowledgeDao(): KnowledgeDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS topics (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS index_topics_title
                    ON topics(title)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS topic_item_cross_refs (
                        topicId INTEGER NOT NULL,
                        itemId INTEGER NOT NULL,
                        addedAt INTEGER NOT NULL,
                        addedBy TEXT NOT NULL,
                        PRIMARY KEY(topicId, itemId),
                        FOREIGN KEY(topicId) REFERENCES topics(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(itemId) REFERENCES data_items(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_topic_item_cross_refs_topicId
                    ON topic_item_cross_refs(topicId)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_topic_item_cross_refs_itemId
                    ON topic_item_cross_refs(itemId)
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS topic_analysis_results (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        topicId INTEGER NOT NULL,
                        summary TEXT NOT NULL,
                        keyPoints TEXT NOT NULL,
                        sourceItemIds TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(topicId) REFERENCES topics(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_topic_analysis_results_topicId
                    ON topic_analysis_results(topicId)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS topic_actions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        topicId INTEGER NOT NULL,
                        analysisResultId INTEGER,
                        type TEXT NOT NULL,
                        title TEXT NOT NULL,
                        body TEXT NOT NULL,
                        status TEXT NOT NULL,
                        editablePayload TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        FOREIGN KEY(topicId) REFERENCES topics(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(analysisResultId) REFERENCES topic_analysis_results(id) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_topic_actions_topicId
                    ON topic_actions(topicId)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_topic_actions_analysisResultId
                    ON topic_actions(analysisResultId)
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS knowledge_table (
                        id TEXT PRIMARY KEY NOT NULL,
                        type TEXT NOT NULL,
                        source TEXT NOT NULL,
                        title TEXT NOT NULL,
                        topic TEXT NOT NULL,
                        purpose TEXT NOT NULL,
                        summary TEXT NOT NULL,
                        keywords TEXT NOT NULL,
                        content TEXT NOT NULL,
                        groupKey TEXT NOT NULL,
                        groupReason TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE data_items ADD COLUMN extractedContent TEXT"
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE data_items ADD COLUMN purpose TEXT")
                db.execSQL("ALTER TABLE data_items ADD COLUMN purposeKeyword TEXT")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 기존 title 단일 unique index 삭제
                db.execSQL("DROP INDEX IF EXISTS index_topics_title")
                // (title, createdAt) 복합 unique index 생성
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS index_topics_title_createdAt
                    ON topics(title, createdAt)
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE topic_actions ADD COLUMN versionHistory TEXT")
            }
        }
    }
}
