package com.samsung.smartclipboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.smartclipboard.domain.model.AgentActionDraft
import com.samsung.smartclipboard.domain.model.CandidateItem
import com.samsung.smartclipboard.domain.model.RetrievalPlan
import com.samsung.smartclipboard.domain.model.TopicActionStatus
import com.samsung.smartclipboard.domain.repository.DataRepository
import com.samsung.smartclipboard.domain.tool.ToolExecutor
import com.samsung.smartclipboard.domain.tool.ToolRouter
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

data class DraftVersion(
    val version: Int,
    val parentVersion: Int,
    val title: String,
    val body: String,
    val description: String
)

data class ActionReviewUiState(
    val title: String = "",
    val body: String = "",

    val currentVersion: Int = 1,
    val parentVersion: Int = 0,
    val history: List<DraftVersion> = emptyList(),
    val isVersionMenuExpanded: Boolean = false,

    val isEditing: Boolean = false,
    val refineInput: String = "",
    val isRefining: Boolean = false,
    val isExecuted: Boolean = false,
    val isExecuting: Boolean = false,

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

    data class RevertToVersion(val version: Int) : ActionReviewIntent
    data class ToggleVersionMenu(val expanded: Boolean) : ActionReviewIntent

    data object ConfirmExecution : ActionReviewIntent
    data object DismissError : ActionReviewIntent
}

@HiltViewModel
class ActionReviewViewModel @Inject constructor(
    private val dataRepository: DataRepository,
    private val refineAgent: GeminiRefineAgent,
    private val toolRouter: ToolRouter,
    private val toolExecutor: ToolExecutor
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActionReviewUiState())
    val uiState: StateFlow<ActionReviewUiState> = _uiState.asStateFlow()

    private var currentActionDraft: AgentActionDraft? = null
    private var currentActionId: Long? = null
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
            is ActionReviewIntent.RevertToVersion -> revertToVersion(intent.version)
            is ActionReviewIntent.ToggleVersionMenu -> _uiState.update { it.copy(isVersionMenuExpanded = intent.expanded) }
            is ActionReviewIntent.ConfirmExecution -> confirmExecution()
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
            if (wasEditing) {
                val currentHistoryItem = state.history.find { it.version == state.currentVersion }
                if (currentHistoryItem != null && (currentHistoryItem.title != state.title || currentHistoryItem.body != state.body)) {
                    val newVersionNum = (state.history.maxOfOrNull { it.version } ?: 1) + 1
                    val newVersion = DraftVersion(newVersionNum, state.currentVersion, state.title, state.body, "직접 편집됨")

                    updateCurrentDraft(state.title, state.body)

                    state.copy(
                        isEditing = false,
                        currentVersion = newVersionNum,
                        parentVersion = state.currentVersion,
                        history = state.history + newVersion
                    )
                } else {
                    state.copy(isEditing = false)
                }
            } else {
                state.copy(isEditing = true)
            }
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
                    currentActionId = matchedAction.id
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

                    val initialVersion = DraftVersion(1, 0, matchedAction.title, matchedAction.body, "원본 초안")

                    _uiState.update {
                        it.copy(
                            title = matchedAction.title,
                            body = matchedAction.body,
                            currentVersion = 1,
                            parentVersion = 0,
                            history = listOf(initialVersion),
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

    private fun revertToVersion(targetVersionId: Int) {
        val state = _uiState.value
        val targetVersion = state.history.find { it.version == targetVersionId }

        if (targetVersion != null) {
            updateCurrentDraft(targetVersion.title, targetVersion.body)

            _uiState.update {
                it.copy(
                    title = targetVersion.title,
                    body = targetVersion.body,
                    currentVersion = targetVersion.version,
                    parentVersion = targetVersion.parentVersion,
                    isVersionMenuExpanded = false,
                    isEditing = false
                )
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
                val refinedActions = refineAgent.refineActions(tq, pl, sItems, currentActions, feedback).getOrElse { throw it }

                if (refinedActions.isEmpty()) {
                    setFailed("refine", "AI 보완에 실패했습니다. 기존 텍스트를 유지합니다.")
                    return@launch
                }

                val refinedDraft = refinedActions.first()

                currentActionDraft = refinedDraft

                val newVersionNum = (_uiState.value.history.maxOfOrNull { it.version } ?: 1) + 1
                val shortFeedback = if (feedback.length > 10) feedback.substring(0, 10) + "..." else feedback
                val newVersion = DraftVersion(
                    version = newVersionNum,
                    parentVersion = cur.currentVersion,
                    title = refinedDraft.title,
                    body = refinedDraft.body,
                    description = shortFeedback
                )

                _uiState.update {
                    it.copy(
                        isRefining = false,
                        refineInput = "",
                        currentVersion = newVersionNum,
                        parentVersion = cur.currentVersion,
                        history = it.history + newVersion,
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

    private fun updateCurrentDraft(newTitle: String, newBody: String) {
        val original = currentActionDraft ?: return
        currentActionDraft = AgentActionDraft(
            type = original.type,
            confidence = original.confidence,
            reason = original.reason,
            title = newTitle,
            body = newBody,
            payload = original.payload,
            sourceItemIds = original.sourceItemIds
        )
    }

    private fun confirmExecution() {
        val draft = currentActionDraft
        if (draft == null) {
            _uiState.update { it.copy(errorMessage = "실행할 작업이 없습니다.") }
            return
        }

        _uiState.update { it.copy(isExecuting = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                // 1. ToolRouter로 action → tool 매핑
                val routeResult = toolRouter.route(draft).getOrElse { error ->
                    setFailed("route", "도구 매핑 실패: ${error.message}")
                    return@launch
                }

                // 2. 필수 입력 누락 확인
                if (routeResult.missingRequiredInputs.isNotEmpty()) {
                    val missingKeys = routeResult.missingRequiredInputs.joinToString(", ") { it.label }
                    setFailed("route", "필수 항목 누락: $missingKeys")
                    return@launch
                }

                // 3. ToolExecutor로 실제 외부 앱 실행
                val sessionId = "action_${currentActionId ?: System.currentTimeMillis()}"
                val executionResult = toolExecutor.execute(
                    sessionId = sessionId,
                    action = routeResult.action,
                    toolSpec = routeResult.toolSpec,
                    payload = routeResult.resolvedPayload
                )

                if (executionResult.success) {
                    // 4. DB 상태를 EXECUTED로 업데이트
                    currentActionId?.let { id ->
                        dataRepository.updateActionStatus(id, TopicActionStatus.EXECUTED)
                    }
                    _uiState.update { it.copy(isExecuting = false, isExecuted = true) }
                } else {
                    setFailed("execute", executionResult.message)
                }
            } catch (e: Exception) {
                setFailed("execute", "실행 중 오류가 발생했습니다: ${e.message}")
            }
        }
    }

    private fun setFailed(step: String, message: String) {
        _uiState.update {
            it.copy(isRefining = false, isLoading = false, isExecuting = false, errorMessage = message)
        }
    }
}
