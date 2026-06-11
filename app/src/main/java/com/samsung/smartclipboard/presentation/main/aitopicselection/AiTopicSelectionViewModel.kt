package com.samsung.smartclipboard.presentation.main.aitopicselection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.smartclipboard.data.source.local.CollectionPeriodPreferences
import com.samsung.smartclipboard.di.IoDispatcher
import com.samsung.smartclipboard.domain.model.DataCluster
import com.samsung.smartclipboard.domain.repository.DataRepository
import com.samsung.smartclipboard.domain.retrieval.DataClusterer
import com.samsung.smartclipboard.gemini.GeminiClusterTopicAgent
import com.samsung.smartclipboard.presentation.AnalysisStep
import com.samsung.smartclipboard.presentation.StepStatus
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
import kotlinx.coroutines.delay
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
    val errorMessage: String? = null,
    val loadingSteps: List<AnalysisStep> = emptyList(),
    val creatingSteps: List<AnalysisStep> = emptyList(),
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
class AiTopicSelectionViewModel @Inject constructor(
    private val dataRepository: DataRepository,
    private val dataClusterer: DataClusterer,
    private val clusterTopicAgent: GeminiClusterTopicAgent,
    private val collectionPeriodPreferences: CollectionPeriodPreferences,
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
                    errorMessage = null,
                    loadingSteps = listOf(
                        AnalysisStep("수집 데이터 확인 중", StepStatus.Running),
                        AnalysisStep("패턴 분류 대기", StepStatus.Pending),
                        AnalysisStep("추천 주제 준비 대기", StepStatus.Pending),
                    ),
                )
            }

            try {
                val period = collectionPeriodPreferences.collectionPeriod.first()
                val items = withContext(ioDispatcher) {
                    dataRepository.observeItemsInPeriod(period.startDateMs, period.endDateMs).first()
                }
                delay(650)
                if (items.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            suggestions = emptyList(),
                            clusters = emptyList(),
                            selectedSuggestionId = null,
                            errorMessage = "수집된 데이터가 없습니다.",
                            loadingSteps = listOf(
                                AnalysisStep("수집 데이터 확인 실패", StepStatus.Failed),
                                AnalysisStep("패턴 분류 대기", StepStatus.Pending),
                                AnalysisStep("추천 주제 준비 대기", StepStatus.Pending),
                            ),
                        )
                    }
                    return@launch
                }else{
                    _uiState.update {
                        it.copy(
                            loadingSteps = listOf(
                                AnalysisStep("수집 데이터 확인 완료", StepStatus.Success),
                                AnalysisStep("패턴 분류 중", StepStatus.Running),
                                AnalysisStep("추천 주제 준비 대기", StepStatus.Pending),
                            ),
                        )
                    }
                }


                val clusters = withContext(ioDispatcher) {
                    dataClusterer.cluster(items)
                }
                delay(650)
                if (clusters.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            suggestions = emptyList(),
                            clusters = emptyList(),
                            selectedSuggestionId = null,
                            errorMessage = "추천할 데이터 묶음을 찾지 못했습니다.",
                            loadingSteps = listOf(
                                AnalysisStep("수집 데이터 확인 완료", StepStatus.Success),
                                AnalysisStep("패턴 분류 실패", StepStatus.Failed),
                                AnalysisStep("추천 주제 준비 대기", StepStatus.Pending),
                            ),
                        )
                    }
                    return@launch
                }else{
                    _uiState.update {
                        it.copy(
                            loadingSteps = listOf(
                                AnalysisStep("수집 데이터 확인 완료", StepStatus.Success),
                                AnalysisStep("패턴 분류 완료", StepStatus.Success),
                                AnalysisStep("추천 주제 준비 중", StepStatus.Running),
                            ),
                        )
                    }
                }


                val refinedClusters = withContext(ioDispatcher) {
                    clusterTopicAgent.suggestTopics(clusters, items).getOrElse { clusters }
                }
                val suggestions = refinedClusters
                    .toTopicAiSuggestionUi()
                    .rankForQuery(normalizedQuery)
                delay(650)
                if (suggestions.isEmpty()){
                    _uiState.update {
                        it.copy(
                            loadingSteps = listOf(
                                AnalysisStep("수집 데이터 확인 완료", StepStatus.Success),
                                AnalysisStep("패턴 분류 완료", StepStatus.Success),
                                AnalysisStep("추천 주제 준비 실패", StepStatus.Failed),
                            ),
                        )
                    }
                    return@launch
                }else{
                    _uiState.update {
                        it.copy(
                            loadingSteps = listOf(
                                AnalysisStep("수집 데이터 확인 완료", StepStatus.Success),
                                AnalysisStep("패턴 분류 완료", StepStatus.Success),
                                AnalysisStep("추천 주제 준비 완료", StepStatus.Success),
                            ),
                        )
                    }
                }

                delay(650)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        suggestions = suggestions,
                        clusters = refinedClusters,
                        selectedSuggestionId = null,
                        errorMessage = if (suggestions.isEmpty()) "추천 주제를 찾지 못했습니다." else null,
                        loadingSteps = emptyList(),
                    )
                }

            } catch (e: Exception) {
                val currentSteps = _uiState.value.loadingSteps
                val failedIndex = currentSteps.indexOfFirst { it.status == StepStatus.Running }
                    .coerceAtLeast(0)
                val updatedSteps = currentSteps.mapIndexed { index, step ->
                    when {
                        index < failedIndex -> step.copy(status = StepStatus.Success)
                        index == failedIndex -> step.copy(status = StepStatus.Failed)
                        else -> step.copy(status = StepStatus.Pending)
                    }
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        suggestions = emptyList(),
                        clusters = emptyList(),
                        selectedSuggestionId = null,
                        errorMessage = e.message ?: "AI 추천 주제 생성에 실패했습니다.",
                        loadingSteps = updatedSteps,
                    )
                }
            }
        }
    }

    fun resetLoadingSteps() {
        _uiState.update {
            it.copy(
                isLoading = false,
                errorMessage = null,
                loadingSteps = emptyList(),
            )
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
                    errorMessage = null,
                    creatingSteps = listOf(
                        AnalysisStep("선택한 데이터 불러오는 중", StepStatus.Running),
                        AnalysisStep("텍스트와 이미지 분석 대기", StepStatus.Pending),
                        AnalysisStep("실행 초안 준비 대기", StepStatus.Pending),
                    ),
                )
            }

            try {
                val itemIds = suggestion.itemIds.distinct()
                delay(650)
                if (itemIds.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isCreatingTopic = false,
                            selectedSuggestionId = null,
                            errorMessage = "추천 주제에 연결할 데이터가 없습니다.",
                            creatingSteps = listOf(
                                AnalysisStep("선택한 데이터 불러오기 실패", StepStatus.Failed),
                                AnalysisStep("텍스트와 이미지 분석 대기", StepStatus.Pending),
                                AnalysisStep("실행 초안 준비 대기", StepStatus.Pending),
                            ),
                        )
                    }
                    return@launch
                }else{
                    _uiState.update {
                        it.copy(
                            creatingSteps = listOf(
                                AnalysisStep("선택한 데이터 불러오기 성공", StepStatus.Success),
                                AnalysisStep("텍스트와 이미지 분석 중", StepStatus.Running),
                                AnalysisStep("실행 초안 준비 대기", StepStatus.Pending),
                            ),
                        )
                    }
                }

                val createdTopicId = withContext(ioDispatcher) {
                    dataRepository.addItemsToTopic(
                        title = suggestion.title,
                        itemIds = itemIds,
                        addedBy = "AI_CLUSTER"
                    )
                }
                val analysisCreated = withContext(ioDispatcher) {
                    dataRepository.runTopicAnalysis(createdTopicId)
                }
                delay(650)
                _uiState.update {
                    it.copy(
                        creatingSteps = listOf(
                            AnalysisStep("선택한 데이터 불러오기 성공", StepStatus.Success),
                            AnalysisStep("텍스트와 이미지 분석 성공", StepStatus.Success),
                            AnalysisStep("실행 초안 준비 중", StepStatus.Running),
                        ),
                    )
                }

                // Step 3: 실행 초안 생성
                val hasActions = withContext(ioDispatcher) {
                    dataRepository.observeTopicActions(createdTopicId).first().isNotEmpty()
                }
                if (!analysisCreated && !hasActions) {
                    throw IllegalStateException("Topic 분석에 실패했습니다.")
                }
                if (!hasActions) {
                    throw IllegalStateException("Topic action 초안을 만들지 못했습니다.")
                }
                delay(650)
                _uiState.update {
                    it.copy(
                        creatingSteps = listOf(
                            AnalysisStep("선택한 데이터 불러오기 성공", StepStatus.Success),
                            AnalysisStep("텍스트와 이미지 분석 성공", StepStatus.Success),
                            AnalysisStep("실행 초안 준비 성공", StepStatus.Success),
                        ),
                    )
                }
                delay(650)

                _uiState.update {
                    it.copy(
                        isCreatingTopic = false,
                        selectedSuggestionId = null,
                        creatingSteps = emptyList(),
                    )
                }
                _effects.send(
                    TopicAiSuggestEffect.NavigateToTopicDetail(
                        topicId = createdTopicId,
                        topicTitle = suggestion.title,
                        query = _uiState.value.query
                    )
                )
            } catch (e: Exception) {
                val currentSteps = _uiState.value.creatingSteps
                val failedIndex = currentSteps.indexOfFirst { it.status == StepStatus.Running }
                    .coerceAtLeast(0)
                val updatedSteps = currentSteps.mapIndexed { index, step ->
                    when {
                        index < failedIndex -> step.copy(status = StepStatus.Success)
                        index == failedIndex -> step.copy(status = StepStatus.Failed)
                        else -> step.copy(status = StepStatus.Pending)
                    }
                }

                _uiState.update {
                    it.copy(
                        isCreatingTopic = false,
                        selectedSuggestionId = null,
                        errorMessage = e.message ?: "추천 주제로 Topic을 만드는 데 실패했습니다.",
                        creatingSteps = updatedSteps,
                    )
                }
            }
        }
    }

    fun resetCreatingSteps() {
        _uiState.update {
            it.copy(
                isCreatingTopic = false,
                selectedSuggestionId = null,
                errorMessage = null,
                creatingSteps = emptyList(),
            )
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
    }.distinctBy { it.title to it.id }
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