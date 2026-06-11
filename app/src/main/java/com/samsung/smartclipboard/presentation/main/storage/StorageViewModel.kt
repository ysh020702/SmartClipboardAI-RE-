package com.samsung.smartclipboard.presentation.main.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.smartclipboard.data.source.CollectionPeriod
import com.samsung.smartclipboard.data.source.CollectionPeriodPreferences
import com.samsung.smartclipboard.domain.repository.DataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StorageUiState(
    val collectionPeriod: CollectionPeriod = CollectionPeriod(),
    val itemCountInPeriod: Int = 0,
    val totalItemCount: Int = 0,
)

@HiltViewModel
class StorageViewModel @Inject constructor(
    private val collectionPeriodPreferences: CollectionPeriodPreferences,
    private val dataRepository: DataRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StorageUiState())
    val uiState: StateFlow<StorageUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                collectionPeriodPreferences.collectionPeriod,
                dataRepository.observeItems()
            ) { period, _ ->
                period
            }.collect { period ->
                val countInPeriod = dataRepository.getItemCount(period.startDateMs, period.endDateMs)
                val totalCount = dataRepository.getItemCount(null, null)
                _uiState.update { state ->
                    state.copy(
                        collectionPeriod = period,
                        itemCountInPeriod = countInPeriod,
                        totalItemCount = totalCount,
                    )
                }
            }
        }
    }

    fun setStartDate(dateMs: Long?) {
        viewModelScope.launch {
            collectionPeriodPreferences.setStartDate(dateMs)
        }
    }

    fun setEndDate(dateMs: Long?) {
        viewModelScope.launch {
            collectionPeriodPreferences.setEndDate(dateMs)
        }
    }

    fun setCollectionPeriod(startDateMs: Long?, endDateMs: Long?) {
        viewModelScope.launch {
            collectionPeriodPreferences.setCollectionPeriod(startDateMs, endDateMs)
        }
    }
}