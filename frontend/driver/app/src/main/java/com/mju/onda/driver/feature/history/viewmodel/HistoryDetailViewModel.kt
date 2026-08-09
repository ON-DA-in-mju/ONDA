package com.mju.onda.driver.feature.history.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.feature.history.data.HistoryDetailInfo
import com.mju.onda.driver.feature.history.data.MockHistoryDetail
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HistoryDetailUiState(
    val detail: HistoryDetailInfo = MockHistoryDetail.forRecordId("h1"),
)

sealed interface HistoryDetailEvent {
    data object NavigateBack : HistoryDetailEvent
    data object GoToTodayHome : HistoryDetailEvent
}

class HistoryDetailViewModel(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val recordId: String =
        savedStateHandle.get<String>("recordId") ?: "h1"

    private val _uiState = MutableStateFlow(
        HistoryDetailUiState(detail = MockHistoryDetail.forRecordId(recordId)),
    )
    val uiState: StateFlow<HistoryDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HistoryDetailEvent>()
    val events: SharedFlow<HistoryDetailEvent> = _events.asSharedFlow()

    fun onBack() {
        viewModelScope.launch { _events.emit(HistoryDetailEvent.NavigateBack) }
    }

    fun onHome() {
        viewModelScope.launch { _events.emit(HistoryDetailEvent.GoToTodayHome) }
    }
}
