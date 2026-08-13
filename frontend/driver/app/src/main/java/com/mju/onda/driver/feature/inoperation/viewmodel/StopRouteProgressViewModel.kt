package com.mju.onda.driver.feature.inoperation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.core.location.OperationStopProgressCoordinator
import com.mju.onda.driver.feature.inoperation.data.StopRouteProgress
import com.mju.onda.driver.feature.inoperation.data.StopRouteProgressState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StopRouteProgressUiState(
    val progress: StopRouteProgressState = StopRouteProgress.initial(
        routeName = "",
        vehicleName = "",
        stops = emptyList(),
    ),
    val loadingStops: Boolean = false,
)

sealed interface StopRouteProgressEvent {
    data object NavigateBack : StopRouteProgressEvent
}

class StopRouteProgressViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(StopRouteProgressUiState())
    val uiState: StateFlow<StopRouteProgressUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<StopRouteProgressEvent>()
    val events: SharedFlow<StopRouteProgressEvent> = _events.asSharedFlow()

    private var collectJob: Job? = null

    fun load(operationId: String) {
        OperationStopProgressCoordinator.attach(operationId)
        _uiState.value = StopRouteProgressUiState(
            progress = OperationStopProgressCoordinator.uiState.value,
            loadingStops = OperationStopProgressCoordinator.loadingStops.value,
        )
        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            combine(
                OperationStopProgressCoordinator.uiState,
                OperationStopProgressCoordinator.loadingStops,
            ) { progress, loading ->
                StopRouteProgressUiState(progress = progress, loadingStops = loading)
            }.collect { next ->
                _uiState.update { next }
            }
        }
    }

    fun onBack() {
        viewModelScope.launch { _events.emit(StopRouteProgressEvent.NavigateBack) }
    }
}
