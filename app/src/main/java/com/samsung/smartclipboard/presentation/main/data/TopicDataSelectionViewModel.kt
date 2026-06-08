package com.samsung.smartclipboard.presentation.main.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.smartclipboard.di.IoDispatcher
import com.samsung.smartclipboard.domain.model.DataItem
import com.samsung.smartclipboard.domain.repository.DataRepository
import com.samsung.smartclipboard.gemini.GeminiFindData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class TopicDataSelectionUiState(
    val items: List<DataItem> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val isLoading: Boolean = true,
    val isAiRecommending: Boolean = false,
    val isCreatingTask: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface TopicDataSelectionEffect {
    data class NavigateToTopicDetail(
        val topicId: Long,
        val topicTitle: String,
    ) : TopicDataSelectionEffect
}

@HiltViewModel
class TopicDataSelectionViewModel @Inject constructor(
    private val dataRepository: DataRepository,
    private val geminiFindData: GeminiFindData,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TopicDataSelectionUiState())
    val uiState: StateFlow<TopicDataSelectionUiState> = _uiState.asStateFlow()

    private val _effects = Channel<TopicDataSelectionEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        observeDataItems()
    }

    private fun observeDataItems() {
        viewModelScope.launch {
            dataRepository.observeItems()
                .catch { throwable ->
                    throwable.printStackTrace()
                }
                .collectLatest { items ->
                    _uiState.update { state ->
                        val validIds = items.map { it.id }.toSet()
                        state.copy(
                            items = items,
                            selectedIds = state.selectedIds.intersect(validIds),
                            isLoading = false,
                        )
                    }
                }
        }
    }

    /**
     * Gemini를 사용해 주제와 관련된 데이터를 AI가 추천하여 자동으로 체크합니다.
     * 화면 진입 시 한 번만 호출됩니다.
     */
    fun recommendRelevantItems(topicTitle: String) {
        val currentItems = _uiState.value.items
        if (currentItems.isEmpty() || _uiState.value.isAiRecommending) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(isAiRecommending = true, errorMessage = null)
            }

            try {
                val recommendedIds = withContext(ioDispatcher) {
                    geminiFindData.findRelevantItems(topicTitle, currentItems)
                }

                _uiState.update {
                    it.copy(
                        selectedIds = recommendedIds,
                        isAiRecommending = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAiRecommending = false,
                        errorMessage = "AI 추천 실패: ${e.message}",
                    )
                }
            }
        }
    }

    fun toggleSelected(id: Long) {
        _uiState.update { state ->
            val nextSelected = if (id in state.selectedIds) {
                state.selectedIds - id
            } else {
                state.selectedIds + id
            }
            state.copy(selectedIds = nextSelected)
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            state.copy(selectedIds = state.items.map { it.id }.toSet())
        }
    }

    fun clearSelected() {
        _uiState.update { state ->
            state.copy(selectedIds = emptySet())
        }
    }

    fun getSelectedItems(): List<DataItem> {
        return _uiState.value.items.filter { it.id in _uiState.value.selectedIds }
    }

    /**
     * 선택한 데이터로 Topic을 생성하고 실행 초안을 만든 뒤 TopicDetail 화면으로 이동합니다.
     * AI 주제 추천 파이프라인의 selectSuggestion과 동일한 로직을 사용합니다.
     */
    fun createTaskAndAnalyze(topicTitle: String) {
        val currentState = _uiState.value
        if (currentState.isCreatingTask) return

        val itemIds = currentState.selectedIds.toList()
        if (itemIds.isEmpty()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isCreatingTask = true,
                    errorMessage = null,
                )
            }

            try {
                val topicId = withContext(ioDispatcher) {
                    val createdTopicId = dataRepository.addItemsToTopic(
                        title = topicTitle,
                        itemIds = itemIds,
                        addedBy = "USER",
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
                        isCreatingTask = false,
                        errorMessage = null,
                    )
                }

                _effects.send(
                    TopicDataSelectionEffect.NavigateToTopicDetail(
                        topicId = topicId,
                        topicTitle = topicTitle,
                    )
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isCreatingTask = false,
                        errorMessage = e.message ?: "실행 초안 생성에 실패했습니다.",
                    )
                }
            }
        }
    }
}