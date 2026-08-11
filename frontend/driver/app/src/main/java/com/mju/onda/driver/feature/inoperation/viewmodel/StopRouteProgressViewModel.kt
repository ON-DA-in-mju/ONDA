package com.mju.onda.driver.feature.inoperation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.core.location.LatestLocationHolder
import com.mju.onda.driver.feature.home.data.MockTodayOperations
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder
import com.mju.onda.driver.feature.inoperation.data.RouteStopsCatalog
import com.mju.onda.driver.feature.inoperation.data.StopRouteProgress
import com.mju.onda.driver.feature.inoperation.data.StopRouteProgressState
import com.mju.onda.driver.feature.operation.data.MockOperationDetail
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StopRouteProgressUiState(
    val progress: StopRouteProgressState = StopRouteProgress.initial(
        routeName = "",
        vehicleName = "",
        stops = emptyList(),
    ),
)

sealed interface StopRouteProgressEvent {
    data object NavigateBack : StopRouteProgressEvent
}

class StopRouteProgressViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(StopRouteProgressUiState())
    val uiState: StateFlow<StopRouteProgressUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<StopRouteProgressEvent>()
    val events: SharedFlow<StopRouteProgressEvent> = _events.asSharedFlow()

    private var operationId: String = ""
    private var routeName: String = ""
    private var vehicleName: String = ""
    private var maxReachedIndex: Int = 0
    private var collectJob: Job? = null

    fun load(operationId: String) {
        this.operationId = operationId
        val op = MockTodayOperations.assignedOperations.find { it.id == operationId }
        val detail = MockOperationDetail.forOperationId(operationId)
        routeName = op?.routeName ?: detail.routeName
        vehicleName = op?.vehicleName ?: detail.vehicleName
        maxReachedIndex = 0
        refresh()
        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            LatestLocationHolder.latestFlow.collect { refresh() }
        }
    }

    private fun refresh() {
        val stops = RouteStopsCatalog.stopsForRouteName(routeName)
        val fix = LatestLocationHolder.latest
        val useFix = fix != null && (
            fix.operationId.isBlank() ||
                fix.operationId == operationId ||
                OperationRuntimeStateHolder.isInProgress(operationId)
            )
        val (state, reached) = StopRouteProgress.resolve(
            routeName = routeName,
            vehicleName = vehicleName,
            stops = stops,
            lat = if (useFix) fix?.latitude else null,
            lng = if (useFix) fix?.longitude else null,
            maxReachedIndex = maxReachedIndex,
        )
        maxReachedIndex = reached
        _uiState.update { it.copy(progress = state) }
    }

    fun onBack() {
        viewModelScope.launch { _events.emit(StopRouteProgressEvent.NavigateBack) }
    }
}
