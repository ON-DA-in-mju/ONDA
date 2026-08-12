package com.mju.onda.driver.feature.startcomplete.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.core.location.LatestLocationHolder
import com.mju.onda.driver.core.location.OperationDeviceStatus
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder
import com.mju.onda.driver.feature.startcomplete.data.MockStartComplete
import com.mju.onda.driver.feature.startcomplete.data.StartCompleteInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class StartCompleteUiState(
    val info: StartCompleteInfo = MockStartComplete.forOperationId(
        OperationRuntimeStateHolder.resolveFocusedOperationId(),
    ),
    val locationStatusValue: String = MockStartComplete.LOCATION_STATUS_VALUE,
    val transmissionOk: Boolean = false,
)

sealed interface StartCompleteEvent {
    data object GoToOperation : StartCompleteEvent
}

class StartCompleteViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(StartCompleteUiState())
    val uiState: StateFlow<StartCompleteUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<StartCompleteEvent>()
    val events: SharedFlow<StartCompleteEvent> = _events.asSharedFlow()

    private var refreshJob: Job? = null
    private var operationId: String =
        OperationRuntimeStateHolder.activeOperationId()
            ?: OperationRuntimeStateHolder.resolveFocusedOperationId()

    init {
        refresh()
    }

    fun refresh() {
        operationId = OperationRuntimeStateHolder.activeOperationId()
            ?: OperationRuntimeStateHolder.resolveFocusedOperationId()
        _uiState.update {
            it.copy(info = MockStartComplete.forOperationId(operationId))
        }
        refreshLive()
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            launch {
                LatestLocationHolder.latestFlow.collect { refreshLive() }
            }
            while (isActive) {
                delay(5_000)
                refreshLive()
            }
        }
    }

    private fun refreshLive() {
        val snap = OperationDeviceStatus.transmissionSnapshot(
            getApplication(),
            operationId,
        )
        _uiState.update {
            it.copy(
                locationStatusValue = snap.shortStatusLabel,
                transmissionOk = snap.isOk,
            )
        }
    }

    fun onGoToOperation() {
        viewModelScope.launch { _events.emit(StartCompleteEvent.GoToOperation) }
    }
}
