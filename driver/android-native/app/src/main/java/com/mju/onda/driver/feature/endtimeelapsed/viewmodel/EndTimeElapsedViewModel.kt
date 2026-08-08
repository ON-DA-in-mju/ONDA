package com.mju.onda.driver.feature.endtimeelapsed.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.core.location.LatestLocationHolder
import com.mju.onda.driver.core.location.OperationDeviceStatus
import com.mju.onda.driver.feature.endtimeelapsed.data.EndTimeElapsedInfo
import com.mju.onda.driver.feature.endtimeelapsed.data.MockEndTimeElapsed
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class EndTimeElapsedUiState(
    val info: EndTimeElapsedInfo = MockEndTimeElapsed.forOperationId(
        OperationRuntimeStateHolder.resolveFocusedOperationId(),
    ),
)

sealed interface EndTimeElapsedEvent {
    data object NavigateBack : EndTimeElapsedEvent
    data object ContinueDriving : EndTimeElapsedEvent
    data object EndOperation : EndTimeElapsedEvent
}

class EndTimeElapsedViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(EndTimeElapsedUiState())
    val uiState: StateFlow<EndTimeElapsedUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EndTimeElapsedEvent>()
    val events: SharedFlow<EndTimeElapsedEvent> = _events.asSharedFlow()

    private var operationId: String = OperationRuntimeStateHolder.resolveFocusedOperationId()
    private var tickJob: Job? = null

    fun load(operationId: String) {
        this.operationId = operationId
        refresh()
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            launch {
                LatestLocationHolder.latestFlow.collect { refresh() }
            }
            while (isActive) {
                delay(1_000)
                refresh()
            }
        }
    }

    private fun refresh() {
        val lastTx = OperationDeviceStatus.transmissionSnapshot(
            getApplication(),
            operationId,
        ).lastTransmissionLabel
        _uiState.value = EndTimeElapsedUiState(
            info = MockEndTimeElapsed.forOperationId(operationId, lastTransmission = lastTx),
        )
    }


    fun onBack() {
        viewModelScope.launch { _events.emit(EndTimeElapsedEvent.NavigateBack) }
    }

    fun onContinue() {
        viewModelScope.launch { _events.emit(EndTimeElapsedEvent.ContinueDriving) }
    }

    fun onEnd() {
        viewModelScope.launch { _events.emit(EndTimeElapsedEvent.EndOperation) }
    }
}
