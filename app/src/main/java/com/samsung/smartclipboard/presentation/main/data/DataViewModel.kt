package com.samsung.smartclipboard.presentation.main.data

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.smartclipboard.data.source.media.ScreenshotImportHandler
import com.samsung.smartclipboard.domain.repository.DataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DataViewModel @Inject constructor(
    private val dataRepository: DataRepository,
    private val screenshotImportHandler: ScreenshotImportHandler
) : ViewModel() {

    private val _uiState = MutableStateFlow(DataUiState())
    val uiState: StateFlow<DataUiState> = _uiState.asStateFlow()

    private var observeItemsJob: Job? = null

    /**
     * DB의 DataItem 목록을 화면 상태와 연동한다.
     *
     * 같은 화면에서 여러 번 호출되어도 중복 collect가 생기지 않도록
     * 기존 Job이 있으면 다시 만들지 않는다.
     */
    fun observeDataItems() {
        if (observeItemsJob?.isActive == true) return

        observeItemsJob = viewModelScope.launch {
            dataRepository.observeItems()
                .catch { throwable ->
                    throwable.printStackTrace()
                }
                .collectLatest { items ->
                    _uiState.update { state ->
                        val validIds = items.map { it.id }.toSet()

                        state.copy(
                            items = items,
                            selected = state.selected.intersect(validIds),
                            deleteTargetId = state.deleteTargetId?.takeIf { it in validIds }
                        )
                    }
                }
        }
    }

    /**
     * 화면 진입 시 최근 스크린샷을 가져온다.
     * 실제 DB 저장은 ScreenshotImportHandler 내부에서 수행된다.
     */
    fun importScreenShot() {
        viewModelScope.launch {
            runCatching {
                screenshotImportHandler.importRecentScreenshots()
            }.onFailure { throwable ->
                throwable.printStackTrace()
            }
        }
    }

    fun changeFilter(filter: String) {
        _uiState.update { state ->
            state.copy(
                activeFilter = filter,
                selected = emptySet()
            )
        }
    }

    fun enterSelectMode() {
        _uiState.update { state ->
            state.copy(
                selectMode = true,
                selected = emptySet(),
                showDeleteConfirm = false,
                deleteTargetId = null
            )
        }
    }

    fun exitSelectMode() {
        _uiState.update { state ->
            state.copy(
                selectMode = false,
                selected = emptySet(),
                showDeleteConfirm = false,
                deleteTargetId = null
            )
        }
    }

    fun toggleSelected(id: Long) {
        _uiState.update { state ->
            if (!state.selectMode) {
                state
            } else {
                val nextSelected = if (id in state.selected) {
                    state.selected - id
                } else {
                    state.selected + id
                }

                state.copy(selected = nextSelected)
            }
        }
    }

    fun selectAll(ids: List<Long>) {
        _uiState.update { state ->
            if (!state.selectMode) {
                state
            } else {
                state.copy(selected = ids.toSet())
            }
        }
    }

    fun clearSelected() {
        _uiState.update { state ->
            state.copy(selected = emptySet())
        }
    }

    fun showDeleteAllConfirm() {
        _uiState.update { state ->
            state.copy(
                showDeleteConfirm = true,
                deleteTargetId = null
            )
        }
    }

    fun hideDeleteAllConfirm() {
        _uiState.update { state ->
            state.copy(showDeleteConfirm = false)
        }
    }

    fun requestDeleteItem(id: Long) {
        _uiState.update { state ->
            state.copy(
                deleteTargetId = id,
                showDeleteConfirm = false
            )
        }
    }

    fun cancelDeleteItem() {
        _uiState.update { state ->
            state.copy(deleteTargetId = null)
        }
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            runCatching {
                dataRepository.deleteItem(id)
            }.onSuccess {
                _uiState.update { state ->
                    state.copy(
                        deleteTargetId = null,
                        selected = state.selected - id
                    )
                }
            }.onFailure { throwable ->
                throwable.printStackTrace()
            }
        }
    }

    fun deleteSelectedItems() {
        val selectedIds = _uiState.value.selected

        if (selectedIds.isEmpty()) return

        viewModelScope.launch {
            runCatching {
                selectedIds.forEach { id ->
                    dataRepository.deleteItem(id)
                }
            }.onSuccess {
                _uiState.update { state ->
                    state.copy(
                        selectMode = false,
                        selected = emptySet(),
                        deleteTargetId = null,
                        showDeleteConfirm = false
                    )
                }
            }.onFailure { throwable ->
                throwable.printStackTrace()
            }
        }
    }

    fun deleteAllItems() {
        viewModelScope.launch {
            runCatching {
                dataRepository.clearAll()
            }.onSuccess {
                _uiState.update { state ->
                    state.copy(
                        selectMode = false,
                        selected = emptySet(),
                        showDeleteConfirm = false,
                        deleteTargetId = null
                    )
                }
            }.onFailure { throwable ->
                throwable.printStackTrace()
            }
        }
    }
}


