package com.samsung.smartclipboard.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.smartclipboard.domain.model.Topic
import com.samsung.smartclipboard.domain.model.TopicAction
import com.samsung.smartclipboard.domain.model.TopicActionStatus
import com.samsung.smartclipboard.domain.model.TopicActionType
import com.samsung.smartclipboard.domain.model.TopicAnalysis
import com.samsung.smartclipboard.domain.repository.DataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryDraftUi(
    val actionId: Long,
    val topicId: Long,
    val type: String,
    val icon: ImageVector,
    val color: Color,
    val description: String,
    val status: String,
    val routeActionType: String,
)

data class HistoryTopicUi(
    val id: Long,
    val title: String,
    val date: String,
    val dataCount: Int,
    val summary: String,
    val drafts: List<HistoryDraftUi>,
)

data class HistoryUiState(
    val topics: List<HistoryTopicUi> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

private data class ActionTypeUi(
    val routeActionType: String,
    val typeLabel: String,
    val icon: ImageVector,
    val color: Color,
)

private fun TopicActionType.toActionTypeUi(): ActionTypeUi = when (this) {
    TopicActionType.SUMMARY -> ActionTypeUi("note", "요약 노트", Icons.Default.Description, AppColors.Blue)
    TopicActionType.CALENDAR -> ActionTypeUi("calendar", "캘린더", Icons.Default.CalendarMonth, Color(0xFF2563EB))
    TopicActionType.REMINDER -> ActionTypeUi("reminder", "리마인더", Icons.Default.Notifications, AppColors.BlueDeep)
    TopicActionType.SHARE_DRAFT -> ActionTypeUi("share", "공유", Icons.Default.Share, AppColors.Cyan)
    TopicActionType.TODO -> ActionTypeUi("note", "할 일", Icons.Default.Description, AppColors.Green)
}

private fun TopicActionStatus.toDisplayLabel(): String = when (this) {
    TopicActionStatus.DRAFT -> "초안"
    TopicActionStatus.EDITED -> "수정됨"
    TopicActionStatus.EXECUTED -> "실행됨"
    TopicActionStatus.DISMISSED -> "제외됨"
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("M월 d일 HH:mm", java.util.Locale.KOREAN)
    return sdf.format(java.util.Date(timestamp))
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val dataRepository: DataRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        startObserving()
    }

    private fun startObserving() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            combine(
                dataRepository.observeTopics(),
                dataRepository.observeAllTopicActions(),
                dataRepository.observeAllTopicAnalysis(),
            ) { topics, actions, analysis ->
                buildHistoryUi(topics, actions, analysis)
            }
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "데이터 로드 실패"
                        )
                    }
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    private fun buildHistoryUi(
        topics: List<Topic>,
        actions: List<TopicAction>,
        analysis: List<TopicAnalysis>,
    ): HistoryUiState {
        val topicMap = topics.associateBy { it.id }
        val actionsByTopic = actions.groupBy { it.topicId }
        val analysisByTopic = analysis.groupBy { it.topicId }

        val historyTopics = topics
            .filter { topic -> actionsByTopic[topic.id]?.isNotEmpty() == true }
            .map { topic ->
                val topicActions = actionsByTopic[topic.id].orEmpty()
                val topicAnalysis = analysisByTopic[topic.id].orEmpty()
                val summary = topicAnalysis.firstOrNull()?.summary ?: ""

                HistoryTopicUi(
                    id = topic.id,
                    title = topic.title,
                    date = formatTimestamp(topic.updatedAt),
                    dataCount = topic.itemCount,
                    summary = summary,
                    drafts = topicActions.map { action ->
                        val typeUi = action.type.toActionTypeUi()
                        HistoryDraftUi(
                            actionId = action.id,
                            topicId = action.topicId,
                            type = typeUi.typeLabel,
                            icon = typeUi.icon,
                            color = typeUi.color,
                            description = action.body.take(60),
                            status = action.status.toDisplayLabel(),
                            routeActionType = typeUi.routeActionType,
                        )
                    },
                )
            }

        return HistoryUiState(
            topics = historyTopics,
            isLoading = false,
            errorMessage = null,
        )
    }
}