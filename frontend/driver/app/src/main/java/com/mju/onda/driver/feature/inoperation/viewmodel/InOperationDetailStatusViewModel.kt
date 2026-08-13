package com.mju.onda.driver.feature.inoperation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.core.OperationTripClock
import com.mju.onda.driver.core.location.LatestLocationHolder
import com.mju.onda.driver.core.location.OperationDeviceStatus
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder
import com.mju.onda.driver.feature.inoperation.data.InOperationDetailStatusInfo
import com.mju.onda.driver.feature.inoperation.data.MockInOperationDetailStatus
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

data class InOperationDetailStatusUiState(
    val info: InOperationDetailStatusInfo = InOperationDetailStatusInfo(
        id = "",
        routeName = "",
        vehicleName = "",
        statusLabel = "운행 중",
        actualStartTime = "",
        elapsedLabel = "",
        expectedEndTime = "",
        origin = "",
        destination = "",
        lastTransmission = "",
        networkStatus = "",
        serverStatus = "",
    ),
)

sealed interface InOperationDetailStatusEvent {
    data object NavigateBack : InOperationDetailStatusEvent
    data object GoHome : InOperationDetailStatusEvent
    data object EndOperation : InOperationDetailStatusEvent
    data object SuspendRequest : InOperationDetailStatusEvent
    data object OpenStopRoute : InOperationDetailStatusEvent
}

class InOperationDetailStatusViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(InOperationDetailStatusUiState())
    val uiState: StateFlow<InOperationDetailStatusUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<InOperationDetailStatusEvent>()
    val events: SharedFlow<InOperationDetailStatusEvent> = _events.asSharedFlow()

    private var baseInfo: InOperationDetailStatusInfo = _uiState.value.info
    private var refreshJob: Job? = null

    fun load(operationId: String) {
        baseInfo = MockInOperationDetailStatus.forOperationId(operationId)
        refreshLive()
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            if (baseInfo.routeName.isBlank()) {
                delay(600)
                val retry = MockInOperationDetailStatus.forOperationId(operationId)
                if (retry.routeName.isNotBlank()) {
                    baseInfo = retry
                    refreshLive()
                }
            }
            launch {
                LatestLocationHolder.latestFlow.collect { refreshLive() }
            }
            while (isActive) {
                delay(1_000)
                if (baseInfo.routeName.isBlank()) {
                    val retry = MockInOperationDetailStatus.forOperationId(operationId)
                    if (retry.routeName.isNotBlank()) {
                        baseInfo = retry
                    }
                }
                refreshLive()
            }
        }
    }

    private fun refreshLive() {
        val start = OperationRuntimeStateHolder.ensureStartedAt(baseInfo.id)
            .takeIf { it > 0L }
            ?: System.currentTimeMillis()
        val snap = OperationDeviceStatus.transmissionSnapshot(
            getApplication(),
            baseInfo.id,
        )
        _uiState.update {
            it.copy(
                info = baseInfo.copy(
                    actualStartTime = OperationTripClock.formatHm(start),
                    elapsedLabel = OperationTripClock.formatElapsedMinutes(start),
                    lastTransmission = snap.lastTransmissionLabel,
                    networkStatus = snap.networkLabel,
                    serverStatus = snap.serverLabel,
                    locationBadge = snap.locationBadgeLabel,
                    transmissionOk = snap.isOk,
                ),
            )
        }
    }

    fun onBack() {
        viewModelScope.launch { _events.emit(InOperationDetailStatusEvent.NavigateBack) }
    }

    fun onHome() {
        viewModelScope.launch { _events.emit(InOperationDetailStatusEvent.GoHome) }
    }

    fun onEndOperation() {
        viewModelScope.launch { _events.emit(InOperationDetailStatusEvent.EndOperation) }
    }

    fun onOpenStopRoute() {
        viewModelScope.launch { _events.emit(InOperationDetailStatusEvent.OpenStopRoute) }
    }

    fun onSuspendRequest() {
        viewModelScope.launch { _events.emit(InOperationDetailStatusEvent.SuspendRequest) }
    }
}
