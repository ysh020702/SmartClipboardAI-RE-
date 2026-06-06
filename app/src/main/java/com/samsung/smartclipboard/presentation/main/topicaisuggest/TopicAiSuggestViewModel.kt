package com.samsung.smartclipboard.presentation.main.topicaisuggest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.smartclipboard.di.IoDispatcher
import com.samsung.smartclipboard.domain.model.DataCluster
import com.samsung.smartclipboard.domain.repository.DataRepository
import com.samsung.smartclipboard.domain.retrieval.DataClusterer
import com.samsung.smartclipboard.gemini.GeminiClusterTopicAgent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TopicAiSuggestionUi(
    val id: String,
    val title: String,
    val description: String,
    val confidence: Float,
    val reason: String,
    val clusterLabel: String,
    val itemCount: Int,
    val itemIds: List<Long>,
    val relatedClusterId: String?
)

data class TopicAiSuggestUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val isCreatingTopic: Boolean = false,
    val selectedSuggestionId: String? = null,
    val suggestions: List<TopicAiSuggestionUi> = emptyList(),
    val clusters: List<DataCluster> = emptyList(),
    val errorMessage: String? = null
) {
    val suggestionCount: Int
        get() = suggestions.size

    val canSelectSuggestion: Boolean
        get() = !isLoading && !isCreatingTopic

    fun isCreatingSuggestion(suggestionId: String): Boolean {
        return isCreatingTopic && selectedSuggestionId == suggestionId
    }
}

sealed interface TopicAiSuggestEffect {
    data class NavigateToTopicDetail(
        val topicId: Long,
        val topicTitle: String,
        val query: String
    ) : TopicAiSuggestEffect
}

@HiltViewModel
class TopicAiSuggestViewModel @Inject constructor(
    private val dataRepository: DataRepository,
    private val dataClusterer: DataClusterer,
    private val clusterTopicAgent: GeminiClusterTopicAgent,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(TopicAiSuggestUiState())
    val uiState: StateFlow<TopicAiSuggestUiState> = _uiState.asStateFlow()

    private val _effects = Channel<TopicAiSuggestEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun loadSuggestions(query: String = "", forceRefresh: Boolean = false) {
        val normalizedQuery = query.trim()
        val current = _uiState.value
        if (!forceRefresh && current.query == normalizedQuery && current.suggestions.isNotEmpty()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    query = normalizedQuery,
                    isLoading = true,
                    isCreatingTopic = false,
                    selectedSuggestionId = null,
                    errorMessage = null
                )
            }

            try {
                val items = withContext(ioDispatcher) {
                    dataRepository.observeItems().first()
                }
                if (items.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            suggestions = emptyList(),
                            clusters = emptyList(),
                            selectedSuggestionId = null,
                            errorMessage = "수집된 데이터가 없습니다."
                        )
                    }
                    return@launch
                }

                val clusters = withContext(ioDispatcher) {
                    dataClusterer.cluster(items)
                }
                if (clusters.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            suggestions = emptyList(),
                            clusters = emptyList(),
                            selectedSuggestionId = null,
                            errorMessage = "추천할 데이터 묶음을 찾지 못했습니다."
                        )
                    }
                    return@launch
                }

                val refinedClusters = withContext(ioDispatcher) {
                    clusterTopicAgent.suggestTopics(clusters, items).getOrElse { clusters }
                }
                val suggestions = refinedClusters
                    .toTopicAiSuggestionUi()
                    .rankForQuery(normalizedQuery)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        suggestions = suggestions,
                        clusters = refinedClusters,
                        selectedSuggestionId = null,
                        errorMessage = if (suggestions.isEmpty()) "추천 주제를 찾지 못했습니다." else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        suggestions = emptyList(),
                        clusters = emptyList(),
                        selectedSuggestionId = null,
                        errorMessage = e.message ?: "AI 추천 주제 생성에 실패했습니다."
                    )
                }
            }
        }
    }

    fun selectSuggestion(suggestionId: String) {
        val currentState = _uiState.value
        if (!currentState.canSelectSuggestion) return

        val suggestion = currentState.suggestions.firstOrNull { it.id == suggestionId }
        if (suggestion == null) {
            _uiState.update { it.copy(errorMessage = "선택한 추천 주제를 찾을 수 없습니다.") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isCreatingTopic = true,
                    selectedSuggestionId = suggestion.id,
                    errorMessage = null
                )
            }

            try {
                val itemIds = suggestion.itemIds.distinct()
                if (itemIds.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isCreatingTopic = false,
                            selectedSuggestionId = null,
                            errorMessage = "추천 주제에 연결할 데이터가 없습니다."
                        )
                    }
                    return@launch
                }

                val topicId = withContext(ioDispatcher) {
                    val createdTopicId = dataRepository.addItemsToTopic(
                        title = suggestion.title,
                        itemIds = itemIds,
                        addedBy = "AI_CLUSTER"
                    )
                    val analysisCreated = dataRepository.runTopicAnalysis(createdTopicId)
                    val hasActions = dataRepository.observeTopicActions(createdTopicId).first().isNotEmpty()
                    if (!analysisCreated && !hasActions) {
                        throw IllegalStateException("Topic 분석에 실패했습니다.")
                    }
                    if (!hasActions) {
                        throw IllegalStateException("Topic action 초안을 만들지 못했습니다.")
                    }
                    createdTopicId
                }

                _uiState.update {
                    it.copy(
                        isCreatingTopic = false,
                        selectedSuggestionId = null
                    )
                }
                _effects.send(
                    TopicAiSuggestEffect.NavigateToTopicDetail(
                        topicId = topicId,
                        topicTitle = suggestion.title,
                        query = _uiState.value.query
                    )
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isCreatingTopic = false,
                        selectedSuggestionId = null,
                        errorMessage = e.message ?: "추천 주제로 Topic을 만드는 데 실패했습니다."
                    )
                }
            }
        }
    }
}

internal fun List<DataCluster>.toTopicAiSuggestionUi(): List<TopicAiSuggestionUi> {
    return flatMap { cluster ->
        val clusterLabel = cluster.clusterLabel.ifBlank { "자료 묶음" }
        if (cluster.topicCandidates.isEmpty()) {
            listOf(cluster.toFallbackSuggestion(clusterLabel))
        } else {
            cluster.topicCandidates.mapIndexed { index, topic ->
                TopicAiSuggestionUi(
                    id = "${topic.relatedClusterId ?: cluster.clusterId}:$index",
                    title = topic.suggestedTitle,
                    description = topic.description,
                    confidence = topic.confidence.coerceIn(0.0f, 1.0f),
                    reason = topic.reason,
                    clusterLabel = clusterLabel,
                    itemCount = cluster.itemIds.size,
                    itemIds = cluster.itemIds,
                    relatedClusterId = topic.relatedClusterId ?: cluster.clusterId
                )
            }
        }
    }.distinctBy { it.title }
}

private fun DataCluster.toFallbackSuggestion(clusterLabel: String): TopicAiSuggestionUi {
    return TopicAiSuggestionUi(
        id = "$clusterId:fallback",
        title = "$clusterLabel 정리해줘",
        description = "이 묶음의 ${itemIds.size}개 자료를 바탕으로 요약과 실행 초안을 만들 수 있습니다.",
        confidence = if (itemIds.size >= 2) 0.55f else 0.4f,
        reason = "AI 주제 추천이 없어서 클러스터 라벨을 기반으로 제안했습니다.",
        clusterLabel = clusterLabel,
        itemCount = itemIds.size,
        itemIds = itemIds,
        relatedClusterId = clusterId
    )
}

private fun List<TopicAiSuggestionUi>.rankForQuery(query: String): List<TopicAiSuggestionUi> {
    if (query.isBlank()) return sortedByDescending { it.confidence }

    val tokens = query.lowercase()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }

    return sortedWith(
        compareByDescending<TopicAiSuggestionUi> { suggestion ->
            val haystack = listOf(
                suggestion.title,
                suggestion.description,
                suggestion.reason,
                suggestion.clusterLabel
            ).joinToString(" ").lowercase()
            tokens.count { it in haystack }
        }.thenByDescending { it.confidence }
    )
}
