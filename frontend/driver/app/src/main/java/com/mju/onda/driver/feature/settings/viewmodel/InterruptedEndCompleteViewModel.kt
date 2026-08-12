package com.mju.onda.driver.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.core.OperationTripClock
import com.mju.onda.driver.feature.history.data.HistoryRuntimeStateHolder
import com.mju.onda.driver.feature.home.data.MockTodayOperations
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder
import com.mju.onda.driver.feature.settings.data.InterruptedEndSummary
import com.mju.onda.driver.feature.settings.data.SafeStopHistoryHolder
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class InterruptedEndCompleteUiState(
    val summary: InterruptedEndSummary = InterruptedEndSummary(
        routeName = "",
        vehicleName = "",
        reason = "",
        actualStart = "",
        interruptedAt = "",
        totalDuration = "",
    ),
)

sealed interface InterruptedEndCompleteEvent {
    data object GoToToday : InterruptedEndCompleteEvent
    data object OpenHistory : InterruptedEndCompleteEvent
    data object NavigateBack : InterruptedEndCompleteEvent
}

class InterruptedEndCompleteViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(InterruptedEndCompleteUiState())
    val uiState: StateFlow<InterruptedEndCompleteUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<InterruptedEndCompleteEvent>()
    val events: SharedFlow<InterruptedEndCompleteEvent> = _events.asSharedFlow()

    private var recorded = false

    fun load() {
        val historyItem = SafeStopHistoryHolder.selected()
        val operationId = OperationRuntimeStateHolder.activeOperationId()
            ?: MockTodayOperations.assignedOperations
                .find { it.routeName == historyItem?.routeName && it.vehicleName == historyItem.vehicleName }
                ?.id
            ?: MockTodayOperations.assignedOperations.firstOrNull()?.id
            ?: return
        val operation = MockTodayOperations.assignedOperations.find { it.id == operationId }
            ?: MockTodayOperations.assignedOperations.first()
        val reason = historyItem?.reason ?: "차량 고장"

        if (!recorded) {
            OperationRuntimeStateHolder.endOperation(operationId)
            HistoryRuntimeStateHolder.recordInterruptedEnd(operationId)
            recorded = true
        }

        val start = OperationRuntimeStateHolder.startedAtMillis(operationId)
            ?: System.currentTimeMillis()
        val end = OperationRuntimeStateHolder.endedAtMillis(operationId)
            ?: System.currentTimeMillis()

        _uiState.value = InterruptedEndCompleteUiState(
            summary = InterruptedEndSummary(
                routeName = historyItem?.routeName ?: operation.routeName,
                vehicleName = historyItem?.vehicleName ?: operation.vehicleName,
                reason = reason,
                actualStart = OperationTripClock.formatHm(start),
                interruptedAt = OperationTripClock.formatHm(end),
                totalDuration = OperationTripClock.formatDurationHms(start, end),
            ),
        )
    }

    fun onGoToToday() {
        viewModelScope.launch { _events.emit(InterruptedEndCompleteEvent.GoToToday) }
    }

    fun onOpenHistory() {
        viewModelScope.launch { _events.emit(InterruptedEndCompleteEvent.OpenHistory) }
    }

    fun onBack() {
        viewModelScope.launch { _events.emit(InterruptedEndCompleteEvent.NavigateBack) }
    }
}
