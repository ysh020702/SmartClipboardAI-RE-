package com.samsung.smartclipboard.presentation.main.manualdataselection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.smartclipboard.data.source.CollectionPeriodPreferences
import com.samsung.smartclipboard.di.IoDispatcher
import com.samsung.smartclipboard.domain.model.DataItem
import com.samsung.smartclipboard.domain.repository.DataRepository
import com.samsung.smartclipboard.gemini.GeminiFindData
import com.samsung.smartclipboard.presentation.AnalysisStep
import com.samsung.smartclipboard.presentation.StepStatus
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ManualDataSelectionUiState(
    val items: List<DataItem> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val isLoading: Boolean = true,
    val isAiRecommending: Boolean = false,
    val isCreatingTask: Boolean = false,
    val errorMessage: String? = null,
    val analysisSteps: List<AnalysisStep> = emptyList(),
)

sealed interface ManualDataSelectionEffect {
    data class NavigateToTopicDetail(
        val topicId: Long,
        val topicTitle: String,
    ) : ManualDataSelectionEffect
}

@HiltViewModel
class ManualDataSelectionViewModel @Inject constructor(
    private val dataRepository: DataRepository,
    private val geminiFindData: GeminiFindData,
    private val collectionPeriodPreferences: CollectionPeriodPreferences,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManualDataSelectionUiState())
    val uiState: StateFlow<ManualDataSelectionUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ManualDataSelectionEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        observeDataItems()
    }

    private fun observeDataItems() {
        viewModelScope.launch {
            collectionPeriodPreferences.collectionPeriod
                .collectLatest { period ->
                    dataRepository.observeItemsInPeriod(period.startDateMs, period.endDateMs)
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
     * 선택한 데이터로 Topic을 생성하고 실행 항목을 만든 뒤 TopicDetail 화면으로 이동합니다.
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
                    analysisSteps = listOf(
                        AnalysisStep("선택한 데이터 불러오는 중", StepStatus.Running),
                        AnalysisStep("텍스트와 이미지 분석 대기", StepStatus.Pending),
                        AnalysisStep("실행 항목 준비 대기", StepStatus.Pending),
                    ),
                )
            }

            try {
                val createdTopicId = withContext(ioDispatcher) {
                    dataRepository.addItemsToTopic(
                        title = topicTitle,
                        itemIds = itemIds,
                        addedBy = "USER",
                    )
                }
                delay(650)
                _uiState.update {
                    it.copy(
                        analysisSteps = listOf(
                            AnalysisStep("선택한 데이터 불러오기 성공", StepStatus.Success),
                            AnalysisStep("텍스트와 이미지 분석 중", StepStatus.Running),
                            AnalysisStep("실행 항목 준비 대기", StepStatus.Pending),
                        ),
                    )
                }

                // Step 2: 분석 실행
                val analysisCreated = withContext(ioDispatcher) {
                    dataRepository.runTopicAnalysis(createdTopicId)
                }
                delay(650)
                _uiState.update {
                    it.copy(
                        analysisSteps = listOf(
                            AnalysisStep("선택한 데이터 불러오기 성공", StepStatus.Success),
                            AnalysisStep("텍스트와 이미지 분석 성공", StepStatus.Success),
                            AnalysisStep("실행 항목 준비 중", StepStatus.Running),
                        ),
                    )
                }

                // Step 3: 실행 항목 생성
                val hasActions = withContext(ioDispatcher) {
                    dataRepository.observeTopicActions(createdTopicId).first().isNotEmpty()
                }
                if (!analysisCreated && !hasActions) {
                    throw IllegalStateException("Topic 분석에 실패했습니다.")
                }
                if (!hasActions) {
                    throw IllegalStateException("Topic action 항목을 만들지 못했습니다.")
                }
                delay(650)
                _uiState.update {
                    it.copy(
                        analysisSteps = listOf(
                            AnalysisStep("선택한 데이터 불러오기 성공", StepStatus.Success),
                            AnalysisStep("텍스트와 이미지 분석 성공", StepStatus.Success),
                            AnalysisStep("실행 항목 준비 성공", StepStatus.Success),
                        ),
                    )
                }

                // 마지막 단계 완료 후 1초 딜레이 (사용자가 완료 상태를 인지할 시간)
                delay(650)

                _uiState.update {
                    it.copy(
                        isCreatingTask = false,
                        errorMessage = null,
                        analysisSteps = emptyList(),
                    )
                }

                _effects.send(
                    ManualDataSelectionEffect.NavigateToTopicDetail(
                        topicId = createdTopicId,
                        topicTitle = topicTitle,
                    )
                )
            } catch (e: Exception) {
                // 실패한 단계까지는 Success, 실패한 단계는 Failed, 이후는 Pending
                val currentSteps = _uiState.value.analysisSteps
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
                        isCreatingTask = false,
                        errorMessage = e.message ?: "실행 항목 생성에 실패했습니다.",
                        analysisSteps = updatedSteps,
                    )
                }
            }
        }
    }

    fun resetAnalysisSteps() {
        _uiState.update {
            it.copy(
                isCreatingTask = false,
                errorMessage = null,
                analysisSteps = emptyList(),
            )
        }
    }
}