package com.mju.onda.driver.feature.endcomplete.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.feature.endcomplete.data.EndCompleteSummary
import com.mju.onda.driver.feature.endcomplete.data.MockEndComplete
import com.mju.onda.driver.feature.history.data.HistoryRuntimeStateHolder
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EndCompleteUiState(
    val summary: EndCompleteSummary = MockEndComplete.forOperationId(
        OperationRuntimeStateHolder.resolveFocusedOperationId(),
    ),
)

sealed interface EndCompleteEvent {
    data object GoToTodayOperation : EndCompleteEvent
    data object OpenHistory : EndCompleteEvent
}

class EndCompleteViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(EndCompleteUiState())
    val uiState: StateFlow<EndCompleteUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EndCompleteEvent>()
    val events: SharedFlow<EndCompleteEvent> = _events.asSharedFlow()

    private var operationId: String = "op-0905"

    fun load(operationId: String) {
        this.operationId = operationId
        OperationRuntimeStateHolder.endOperation(operationId)
        HistoryRuntimeStateHolder.recordNormalEnd(operationId)
        _uiState.value = EndCompleteUiState(
            summary = MockEndComplete.forOperationId(operationId),
        )
    }

    fun onGoToToday() {
        viewModelScope.launch { _events.emit(EndCompleteEvent.GoToTodayOperation) }
    }

    fun onHistory() {
        viewModelScope.launch { _events.emit(EndCompleteEvent.OpenHistory) }
    }
}
