package com.samsung.smartclipboard.presentation.main.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.smartclipboard.data.source.CollectionPeriodPreferences
import com.samsung.smartclipboard.data.source.screenshot.ScreenshotImportHandler
import com.samsung.smartclipboard.domain.model.DataItem
import com.samsung.smartclipboard.domain.model.DataItemType
import com.samsung.smartclipboard.domain.model.LinkMetadataCodec
import com.samsung.smartclipboard.domain.repository.DataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DataViewModel @Inject constructor(
    private val dataRepository: DataRepository,
    private val screenshotImportHandler: ScreenshotImportHandler,
    private val collectionPeriodPreferences: CollectionPeriodPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(DataUiState())
    val uiState: StateFlow<DataUiState> = _uiState.asStateFlow()

    private var observeItemsJob: Job? = null
    private val attemptedLinkMetadataIds = mutableSetOf<Long>()

    fun observeDataItems() {
        if (observeItemsJob?.isActive == true) return

        observeItemsJob = viewModelScope.launch {
            collectionPeriodPreferences.collectionPeriod
                .combine(dataRepository.observeItems()) { period, _ -> period }
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
                                    selected = state.selected.intersect(validIds),
                                )
                            }
                            enrichMissingLinkMetadata(items)
                        }
                }
        }
    }

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
            state.copy(activeFilter = filter, selected = emptySet())
        }
    }

    fun enterSelectMode() {
        _uiState.update { state ->
            state.copy(selectMode = true, selected = emptySet())
        }
    }

    fun enterSelectMode(itemId: Long) {
        _uiState.update { state ->
            state.copy(selectMode = true, selected = setOf(itemId))
        }
    }

    fun exitSelectMode() {
        _uiState.update { state ->
            state.copy(selectMode = false, selected = emptySet())
        }
    }

    fun toggleSelected(id: Long) {
        _uiState.update { state ->
            if (!state.selectMode) return@update state
            val nextSelected = if (id in state.selected) state.selected - id else state.selected + id
            state.copy(selected = nextSelected)
        }
    }

    fun selectAll(ids: List<Long>) {
        _uiState.update { state ->
            if (!state.selectMode) return@update state
            state.copy(selected = ids.toSet())
        }
    }

    fun deleteSelectedItems() {
        val selectedIds = _uiState.value.selected
        if (selectedIds.isEmpty()) return

        viewModelScope.launch {
            runCatching {
                selectedIds.forEach { id -> dataRepository.deleteItem(id) }
            }.onSuccess {
                _uiState.update { state ->
                    state.copy(selectMode = false, selected = emptySet())
                }
            }.onFailure { throwable ->
                throwable.printStackTrace()
            }
        }
    }

    private fun enrichMissingLinkMetadata(items: List<DataItem>) {
        val targets = items.filter { item ->
            item.type == DataItemType.LINK &&
                LinkMetadataCodec.decode(item.extractedContent) == null &&
                attemptedLinkMetadataIds.add(item.id)
        }
        if (targets.isEmpty()) return

        viewModelScope.launch {
            targets.forEach { item ->
                runCatching {
                    dataRepository.enrichLinkMetadata(item.id)
                }.onFailure { throwable ->
                    throwable.printStackTrace()
                }
            }
        }
    }
}
