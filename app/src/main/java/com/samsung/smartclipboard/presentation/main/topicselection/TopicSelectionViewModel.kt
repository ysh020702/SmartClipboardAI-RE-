package com.samsung.smartclipboard.presentation.main.topicselection

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.smartclipboard.domain.model.TopicAction
import com.samsung.smartclipboard.domain.model.TopicActionStatus
import com.samsung.smartclipboard.domain.model.TopicActionType
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

data class TopicActionCardUi(
    val actionId: Long,
    val routeActionType: String,
    val typeLabel: String,
    val title: String,
    val description: String,
    val statusLabel: String,
    val icon: ImageVector,
    val color: Color
)

fun TopicAction.toTopicActionCardUi(): TopicActionCardUi {
    val typeUi = when (type) {
        TopicActionType.SUMMARY -> TopicActionTypeUi(
            routeActionType = "note",
            typeLabel = "요약 노트",
            icon = Icons.Default.Description,
            color = AppColors.Blue
        )
        TopicActionType.CALENDAR -> TopicActionTypeUi(
            routeActionType = "calendar",
            typeLabel = "캘린더",
            icon = Icons.Default.CalendarMonth,
            color = Color(0xFF2563EB)
        )
        TopicActionType.REMINDER -> TopicActionTypeUi(
            routeActionType = "reminder",
            typeLabel = "리마인더",
            icon = Icons.Default.Notifications,
            color = AppColors.BlueDeep
        )
        TopicActionType.SHARE_DRAFT -> TopicActionTypeUi(
            routeActionType = "share",
            typeLabel = "공유",
            icon = Icons.Default.Share,
            color = AppColors.Cyan
        )
        TopicActionType.TODO -> TopicActionTypeUi(
            routeActionType = "note",
            typeLabel = "할 일",
            icon = Icons.Default.Description,
            color = AppColors.Green
        )
    }

    return TopicActionCardUi(
        actionId = id,
        routeActionType = typeUi.routeActionType,
        typeLabel = typeUi.typeLabel,
        title = title,
        description = body,
        statusLabel = status.toDisplayLabel(),
        icon = typeUi.icon,
        color = typeUi.color
    )
}

private data class TopicActionTypeUi(
    val routeActionType: String,
    val typeLabel: String,
    val icon: ImageVector,
    val color: Color
)

private fun TopicActionStatus.toDisplayLabel(): String {
    return when (this) {
        TopicActionStatus.DRAFT -> "초안"
        TopicActionStatus.EDITED -> "수정됨"
        TopicActionStatus.EXECUTED -> "실행됨"
        TopicActionStatus.DISMISSED -> "제외됨"
    }
}

data class TopicSelectionUiState(
    val topicId: Long? = null,
    val topicTitle: String? = null,
    val actions: List<TopicActionCardUi> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    val actionCount: Int
        get() = actions.size
}

@HiltViewModel
class TopicSelectionViewModel @Inject constructor(
    private val dataRepository: DataRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TopicSelectionUiState())
    val uiState: StateFlow<TopicSelectionUiState> = _uiState.asStateFlow()

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
                TopicSelectionUiState(
                    topicId = topicId,
                    topicTitle = topicTitle,
                    actions = actions.map { it.toTopicActionCardUi() },
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
