package com.samsung.smartclipboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.smartclipboard.domain.model.AgentActionDraft
import com.samsung.smartclipboard.domain.model.CandidateItem
import com.samsung.smartclipboard.domain.model.RetrievalPlan
import com.samsung.smartclipboard.domain.repository.DataRepository
import com.samsung.smartclipboard.gemini.GeminiRefineAgent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class QuickRefineAction(val label: String, val feedback: String) {
    MORE_CONCISE("더 간결하게", "더 간결하고 짧게 만들어줘. 불필요한 설명은 빼고 핵심만 남겨줘."),
    KEY_SUMMARY("핵심만 요약", "핵심 포인트만 요약해줘. 가장 중요한 내용만 남겨줘."),
    CHANGE_TITLE("제목 바꿔줘", "제목을 더 직관적이고 이해하기 쉽게 바꿔줘."),
    TRANSLATE_EN("영어로 번역", "내용을 영어로 번역해줘. 제목과 본문 모두 영어로 작성해줘.")
}

data class ActionReviewUiState(
    val title: String = "",
    val body: String = "",
    val version: Int = 1,
    val isEditing: Boolean = false,
    val refineInput: String = "",
    val isRefining: Boolean = false,
    val isExecuted: Boolean = false,

    val isLoading: Boolean = false,
    val errorMessage: String? = null,

    val actionType: String = "note",
    val topicId: String = "1",
    val topicTitle: String = "스크린샷 모음",
    val from: String = "",
    val query: String = ""
)

sealed interface ActionReviewIntent {
    data class Initialize(val data: Map<String, String>) : ActionReviewIntent
    data class UpdateTitle(val title: String) : ActionReviewIntent
    data class UpdateBody(val body: String) : ActionReviewIntent
    data object ToggleEditMode : ActionReviewIntent

    data class RefineFeedbackChanged(val feedback: String) : ActionReviewIntent
    data object StartRefinement : ActionReviewIntent
    data class QuickRefine(val action: QuickRefineAction) : ActionReviewIntent
    data object CancelRefinement : ActionReviewIntent

    data object ConfirmExecution : ActionReviewIntent
    data object DismissError : ActionReviewIntent
}

@HiltViewModel
class ActionReviewViewModel @Inject constructor(
    private val dataRepository: DataRepository,
    private val refineAgent: GeminiRefineAgent
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActionReviewUiState())
    val uiState: StateFlow<ActionReviewUiState> = _uiState.asStateFlow()

    private var currentActionDraft: AgentActionDraft? = null
    private var currentSourceItems: List<CandidateItem> = emptyList()

    fun onIntent(intent: ActionReviewIntent) {
        when (intent) {
            is ActionReviewIntent.Initialize -> initializeData(intent.data)
            is ActionReviewIntent.UpdateTitle -> _uiState.update { it.copy(title = intent.title) }
            is ActionReviewIntent.UpdateBody -> _uiState.update { it.copy(body = intent.body) }
            is ActionReviewIntent.ToggleEditMode -> toggleEditMode()
            is ActionReviewIntent.RefineFeedbackChanged -> onRefineFeedbackChanged(intent.feedback)
            is ActionReviewIntent.StartRefinement -> startRefinement()
            is ActionReviewIntent.QuickRefine -> onQuickRefine(intent.action)
            is ActionReviewIntent.CancelRefinement -> onCancelRefinement()
            is ActionReviewIntent.ConfirmExecution -> _uiState.update { it.copy(isExecuted = true) }
            is ActionReviewIntent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun onRefineFeedbackChanged(feedback: String) {
        _uiState.update { it.copy(refineInput = feedback.take(1500)) }
    }

    private fun onCancelRefinement() {
        val current = _uiState.value
        if (current.isRefining) {
            _uiState.update { it.copy(errorMessage = "보완이 진행 중입니다.") }
            return
        }
        _uiState.update { it.copy(refineInput = "", errorMessage = null) }
    }

    private fun toggleEditMode() {
        _uiState.update { state ->
            val wasEditing = state.isEditing
            state.copy(
                isEditing = !wasEditing,
                version = if (wasEditing) state.version else state.version + 1
            )
        }
    }

    private fun initializeData(data: Map<String, String>) {
        val topicIdStr = data["topicId"] ?: "1"

        _uiState.update {
            it.copy(
                actionType = data["actionType"] ?: "note",
                topicId = topicIdStr,
                topicTitle = data["topicTitle"] ?: "스크린샷 모음",
                from = data["from"].orEmpty(),
                query = data["query"].orEmpty()
            )
        }

        val topicId = topicIdStr.toLongOrNull()
        val actionId = data["actionId"]?.toLongOrNull()

        if (topicId != null && actionId != null) {
            fetchActionData(topicId, actionId)
        }
    }

    private fun fetchActionData(topicId: Long, actionId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val topicActions = dataRepository.observeTopicActions(topicId).first()

                val matchedAction = topicActions.find { it.id == actionId }

                if (matchedAction != null) {
                    currentActionDraft = AgentActionDraft(
                        type = matchedAction.type,
                        confidence = 1.0f,
                        reason = "User initiated review from saved action",
                        title = matchedAction.title,
                        body = matchedAction.body,
                        payload = matchedAction.editablePayload,
                        sourceItemIds = emptyList()
                    )

                    val topicItems = dataRepository.observeTopicItems(topicId).first()
                    currentSourceItems = topicItems.map { CandidateItem(it, 1.0f, "토픽 내 항목") }

                    _uiState.update {
                        it.copy(
                            title = matchedAction.title,
                            body = matchedAction.body,
                            isLoading = false
                        )
                    }
                } else {
                    setFailed("fetch", "해당 작업을 찾을 수 없습니다.")
                }
            } catch (e: Exception) {
                setFailed("fetch", e.message ?: "데이터 로드에 실패했습니다.")
            }
        }
    }

    private fun onQuickRefine(action: QuickRefineAction) {
        _uiState.update { it.copy(refineInput = action.feedback) }
        startRefinement()
    }

    private fun startRefinement() {
        val cur = _uiState.value
        if (cur.isLoading || cur.isRefining) return

        val feedback = cur.refineInput.trim()
        if (feedback.isBlank()) {
            _uiState.update { it.copy(errorMessage = "보완 요청 내용을 입력해 주세요.") }
            return
        }

        val tq = cur.query.ifBlank { cur.topicTitle }
        val pl = RetrievalPlan(keywords = listOf(tq), maxResults = 20)

        val originalDraft = currentActionDraft ?: return

        val currentDraftToRefine = AgentActionDraft(
            type = originalDraft.type,
            confidence = originalDraft.confidence,
            reason = originalDraft.reason,
            title = cur.title,
            body = cur.body,
            payload = originalDraft.payload,
            sourceItemIds = originalDraft.sourceItemIds
        )

        val currentActions = listOf(currentDraftToRefine)
        val sItems = currentSourceItems

        _uiState.update { it.copy(isRefining = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                // GeminiRefineAgent에 보완 요청
                val refinedActions = refineAgent.refineActions(tq, pl, sItems, currentActions, feedback).getOrElse { throw it }

                if (refinedActions.isEmpty()) {
                    setFailed("refine", "AI 보완에 실패했습니다. 기존 텍스트를 유지합니다.")
                    return@launch
                }

                val refinedDraft = refinedActions.first()
                currentActionDraft = refinedDraft

                _uiState.update {
                    it.copy(
                        isRefining = false,
                        refineInput = "",
                        version = it.version + 1,
                        title = refinedDraft.title,
                        body = refinedDraft.body,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                setFailed("refine", "AI 보완 중 오류가 발생했습니다: ${e.message}")
            }
        }
    }

    private fun setFailed(step: String, message: String) {
        _uiState.update {
            it.copy(isRefining = false, isLoading = false, errorMessage = message)
        }
    }
}