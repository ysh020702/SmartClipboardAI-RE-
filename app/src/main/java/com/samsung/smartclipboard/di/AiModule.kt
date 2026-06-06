package com.samsung.smartclipboard.di

import com.samsung.smartclipboard.data.repository.KnowledgeRepositoryImpl
import com.samsung.smartclipboard.domain.repository.KnowledgeRepository
import com.samsung.smartclipboard.BuildConfig
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {


    @Binds
    @Singleton
    abstract fun bindKnowledgeRepository(
        impl: KnowledgeRepositoryImpl
    ): KnowledgeRepository


    companion object {
        @Provides
        @Singleton
        @Named("gemini_api_key")
        fun provideGeminiApiKey(): String = BuildConfig.API_KEY
    }
}
