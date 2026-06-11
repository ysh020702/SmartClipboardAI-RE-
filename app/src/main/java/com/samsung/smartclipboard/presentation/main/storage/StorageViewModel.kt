package com.samsung.smartclipboard.presentation.main.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.smartclipboard.data.source.period.CollectionPeriodManager
import com.samsung.smartclipboard.domain.repository.DataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StorageViewModel @Inject constructor(
    private val collectionPeriodManager: CollectionPeriodManager,
    private val dataRepository: DataRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StorageUiState())
    val uiState: StateFlow<StorageUiState> = _uiState.asStateFlow()

    init {
        loadPeriodSettings()
    }

    private fun loadPeriodSettings() {
        _uiState.update { state ->
            state.copy(
                startDate = collectionPeriodManager.startDate,
                endDate = collectionPeriodManager.endDate,
                periodDescription = collectionPeriodManager.getPeriodDescription(),
                dataLoaded = collectionPeriodManager.dataLoaded
            )
        }
    }

    fun setStartDate(timestamp: Long?) {
        collectionPeriodManager.startDate = timestamp
        loadPeriodSettings()
    }

    fun setEndDate(timestamp: Long?) {
        collectionPeriodManager.endDate = timestamp
        loadPeriodSettings()
    }

    fun clearPeriod() {
        collectionPeriodManager.clearPeriod()
        loadPeriodSettings()
    }

    fun showStartDatePicker() {
        _uiState.update { it.copy(showStartDatePicker = true) }
    }

    fun hideStartDatePicker() {
        _uiState.update { it.copy(showStartDatePicker = false) }
    }

    fun showEndDatePicker() {
        _uiState.update { it.copy(showEndDatePicker = true) }
    }

    fun hideEndDatePicker() {
        _uiState.update { it.copy(showEndDatePicker = false) }
    }

    fun resetAndLoadData() {
        viewModelScope.launch {
            collectionPeriodManager.resetDataLoaded()
            _uiState.update { it.copy(dataLoaded = false, isLoading = true) }

            // 기존 데이터 삭제
            dataRepository.deleteAllItems()

            // 데이터 재로딩 플래그 설정
            collectionPeriodManager.markDataLoaded()
            _uiState.update { it.copy(dataLoaded = true, isLoading = false) }
            loadPeriodSettings()
        }
    }
}

data class StorageUiState(
    val startDate: Long? = null,
    val endDate: Long? = null,
    val periodDescription: String = "전체 기간",
    val showStartDatePicker: Boolean = false,
    val showEndDatePicker: Boolean = false,
    val dataLoaded: Boolean = false,
    val isLoading: Boolean = false
)