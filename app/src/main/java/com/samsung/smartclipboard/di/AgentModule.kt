package com.samsung.smartclipboard.di

import android.content.Context
import com.samsung.smartclipboard.gemini.GeminiActionPlanner
import com.samsung.smartclipboard.gemini.GeminiClusterTopicAgent
import com.samsung.smartclipboard.gemini.GeminiClusterer
import com.samsung.smartclipboard.gemini.GeminiItemRecommendationAgent
import com.samsung.smartclipboard.gemini.GeminiPurposeAnalyzer
import com.samsung.smartclipboard.gemini.GeminiTopicPlanner
import com.samsung.smartclipboard.data.retrieval.LocalCandidateItemRanker
import com.samsung.smartclipboard.data.retrieval.LocalClusterer
import com.samsung.smartclipboard.data.retrieval.LocalDataRetriever
import com.samsung.smartclipboard.data.tool.ToolExecutorImpl
import com.samsung.smartclipboard.data.tool.ToolRegistryImpl
import com.samsung.smartclipboard.data.tool.ToolRouterImpl
import com.samsung.smartclipboard.domain.repository.DataRepository
import com.samsung.smartclipboard.domain.retrieval.CandidateItemRanker
import com.samsung.smartclipboard.domain.retrieval.DataClusterer
import com.samsung.smartclipboard.domain.retrieval.DataRetriever
import com.samsung.smartclipboard.domain.tool.ToolExecutor
import com.samsung.smartclipboard.domain.tool.ToolRegistry
import com.samsung.smartclipboard.domain.tool.ToolRouter
import com.samsung.smartclipboard.gemini.GeminiManager
import com.samsung.smartclipboard.gemini.GeminiRefineAgent
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AgentModule {

    @Provides @Singleton
    fun provideTopicPlanner(geminiManager: GeminiManager): GeminiTopicPlanner {
        return GeminiTopicPlanner(geminiManager)
    }

    @Provides @Singleton
    fun provideDataRetriever(dataRepository: DataRepository): DataRetriever {
        return LocalDataRetriever(dataRepository)
    }

    @Provides @Singleton
    fun provideCandidateItemRanker(): CandidateItemRanker {
        return LocalCandidateItemRanker()
    }

    @Provides @Singleton
    fun provideItemRecommendationAgent(geminiManager: GeminiManager): GeminiItemRecommendationAgent {
        return GeminiItemRecommendationAgent(geminiManager)
    }

    @Provides @Singleton
    fun provideActionPlanner(geminiManager: GeminiManager): GeminiActionPlanner {
        return GeminiActionPlanner(geminiManager)
    }

    @Provides @Singleton
    fun provideToolRegistry(): ToolRegistry {
        return ToolRegistryImpl()
    }

    @Provides @Singleton
    fun provideToolRouter(toolRegistry: ToolRegistry): ToolRouter {
        return ToolRouterImpl(toolRegistry)
    }

    @Provides @Singleton
    fun provideToolExecutor(@ApplicationContext context: Context): ToolExecutor {
        return ToolExecutorImpl(context)
    }

    @Provides @Singleton
    fun provideLocalClusterer(): LocalClusterer {
        return LocalClusterer()
    }

    @Provides @Singleton
    fun provideDataClusterer(geminiManager: GeminiManager, localClusterer: LocalClusterer): DataClusterer {
        return GeminiClusterer(geminiManager, localClusterer)
    }

    @Provides @Singleton
    fun provideClusterTopicAgent(geminiManager: GeminiManager): GeminiClusterTopicAgent {
        return GeminiClusterTopicAgent(geminiManager)
    }

    @Provides @Singleton
    fun provideRefineAgent(geminiManager: GeminiManager): GeminiRefineAgent {
        return GeminiRefineAgent(geminiManager)
    }

    @Provides @Singleton
    fun providePurposeAnalyzer(geminiManager: GeminiManager): GeminiPurposeAnalyzer {
        return GeminiPurposeAnalyzer(geminiManager)
    }
}
