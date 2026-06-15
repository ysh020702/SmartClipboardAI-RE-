package com.samsung.smartclipboard.presentation.main.taskselection

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.smartclipboard.domain.model.TaskSelection
import com.samsung.smartclipboard.domain.model.TaskSelectionStatus
import com.samsung.smartclipboard.domain.model.TaskSelectionType
import com.samsung.smartclipboard.domain.repository.DataRepository
import com.samsung.smartclipboard.presentation.AppColors
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TaskSelectionCardUi(
    val actionId: Long,
    val routeActionType: String,
    val typeLabel: String,
    val title: String,
    val description: String,
    val statusLabel: String,
    val statusColor: Pair<Color, Color>,
    val showStatusLabel: Boolean,
    val icon: ImageVector,
    val color: Color
)

fun TaskSelection.toTaskSelectionCardUi(): TaskSelectionCardUi {
    val typeUi = when (type) {
        TaskSelectionType.SUMMARY -> TaskSelectionTypeUi(
            routeActionType = "note",
            typeLabel = "요약 노트",
            icon = Icons.Default.Description,
            color = AppColors.Blue
        )
        TaskSelectionType.CALENDAR -> TaskSelectionTypeUi(
            routeActionType = "calendar",
            typeLabel = "캘린더",
            icon = Icons.Default.CalendarMonth,
            color = Color(0xFF2563EB)
        )
        TaskSelectionType.REMINDER -> TaskSelectionTypeUi(
            routeActionType = "reminder",
            typeLabel = "리마인더",
            icon = Icons.Default.Notifications,
            color = AppColors.BlueDeep
        )
        TaskSelectionType.SHARE_DRAFT -> TaskSelectionTypeUi(
            routeActionType = "share",
            typeLabel = "공유",
            icon = Icons.Default.Share,
            color = AppColors.Cyan
        )
    }

    return TaskSelectionCardUi(
        actionId = id,
        routeActionType = typeUi.routeActionType,
        typeLabel = typeUi.typeLabel,
        title = title,
        description = body,
        statusLabel = status.toDisplayLabel(),
        statusColor = status.toStatusColor(),
        showStatusLabel = status == TaskSelectionStatus.EXECUTED,
        icon = typeUi.icon,
        color = typeUi.color
    )
}

private data class TaskSelectionTypeUi(
    val routeActionType: String,
    val typeLabel: String,
    val icon: ImageVector,
    val color: Color
)

private fun TaskSelectionStatus.toDisplayLabel(): String {
    return when (this) {
        TaskSelectionStatus.DRAFT -> "초안"
        TaskSelectionStatus.EDITED -> "수정됨"
        TaskSelectionStatus.EXECUTED -> "실행됨"
        TaskSelectionStatus.DISMISSED -> "제외됨"
    }
}

private fun TaskSelectionStatus.toStatusColor(): Pair<Color, Color> {
    return when (this) {
        TaskSelectionStatus.DRAFT -> Color(0xFFDBEAFE) to AppColors.Blue           // 초안 = 파란색
        TaskSelectionStatus.EDITED -> Color(0xFFFEF3C7) to Color(0xFFD97706)       // 수정됨 = 노란색
        TaskSelectionStatus.EXECUTED -> Color(0xFFD1FAE5) to AppColors.Green        // 실행됨 = 초록색
        TaskSelectionStatus.DISMISSED -> Color(0xFFF1F5F9) to AppColors.Slate400   // 제외됨 = 회색
    }
}

data class TaskSelectionUiState(
    val topicId: Long? = null,
    val topicTitle: String? = null,
    val actions: List<TaskSelectionCardUi> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    val actionCount: Int
        get() = actions.size
}

@HiltViewModel
class TaskSelectionViewModel @Inject constructor(
    private val dataRepository: DataRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskSelectionUiState())
    val uiState: StateFlow<TaskSelectionUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null
    private var observedTopicId: Long? = null

    fun observeTopic(topicId: Long, fallbackTitle: String?) {
        if (observedTopicId == topicId && observeJob?.isActive == true) return

        observedTopicId = topicId
        observeJob?.cancel()
        _uiState.update {
            it.copy(
                topicId = topicId,
                topicTitle = fallbackTitle,
                actions = emptyList(),
                isLoading = true,
                errorMessage = null
            )
        }

        observeJob = viewModelScope.launch {
            combine(
                dataRepository.observeTopics(),
                dataRepository.observeTopicActions(topicId)
            ) { topics, actions ->
                val topicTitle = topics.firstOrNull { it.id == topicId }?.title ?: fallbackTitle
                TaskSelectionUiState(
                    topicId = topicId,
                    topicTitle = topicTitle,
                    actions = actions.map { it.toTaskSelectionCardUi() },
                    isLoading = false,
                    errorMessage = null
                )
            }
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Failed to load topic actions"
                        )
                    }
                }
                .collectLatest { state ->
                    _uiState.value = state
                }
        }
    }
}
