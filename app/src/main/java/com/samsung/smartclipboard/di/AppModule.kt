package com.samsung.smartclipboard.di

import android.content.Context
import androidx.room.Room
import com.samsung.smartclipboard.database.dao.DataItemDao
import com.samsung.smartclipboard.database.dao.KnowledgeDao
import com.samsung.smartclipboard.database.SmartClipboardDatabase
import com.samsung.smartclipboard.database.dao.TopicDao
import com.samsung.smartclipboard.data.repository.DataRepositoryImpl
import com.samsung.smartclipboard.domain.repository.DataRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindDataRepository(impl: DataRepositoryImpl): DataRepository

    companion object {
        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): SmartClipboardDatabase {
            return Room.databaseBuilder(
                context,
                SmartClipboardDatabase::class.java,
                "smart_clipboard.db"
            )
                .addMigrations(SmartClipboardDatabase.MIGRATION_2_3)
                .addMigrations(SmartClipboardDatabase.MIGRATION_3_4)
                .addMigrations(SmartClipboardDatabase.MIGRATION_4_5)
                .addMigrations(SmartClipboardDatabase.MIGRATION_5_6)
                .addMigrations(SmartClipboardDatabase.MIGRATION_6_7)
                .fallbackToDestructiveMigration()
                .build()
        }

        @Provides
        @Singleton
        fun provideDataItemDao(database: SmartClipboardDatabase): DataItemDao {
            return database.dataItemDao()
        }

        @Provides
        @Singleton
        fun provideTopicDao(database: SmartClipboardDatabase): TopicDao {
            return database.topicDao()
        }

        @Provides
        @Singleton
        fun provideKnowledgeDao(database: SmartClipboardDatabase): KnowledgeDao {
            return database.knowledgeDao()
        }
    }
}
