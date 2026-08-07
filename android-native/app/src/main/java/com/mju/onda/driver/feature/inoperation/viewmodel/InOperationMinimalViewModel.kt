package com.mju.onda.driver.feature.inoperation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.core.OperationTripClock
import com.mju.onda.driver.core.location.LatestLocationHolder
import com.mju.onda.driver.core.location.OperationDeviceStatus
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder
import com.mju.onda.driver.feature.inoperation.data.InOperationMinimalInfo
import com.mju.onda.driver.feature.inoperation.data.MockInOperationMinimal
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

data class InOperationMinimalUiState(
    val info: InOperationMinimalInfo = MockInOperationMinimal.forOperationId(
        OperationRuntimeStateHolder.resolveFocusedOperationId(),
    ),
)

sealed interface InOperationMinimalEvent {
    data object NavigateBack : InOperationMinimalEvent
    data object GoHome : InOperationMinimalEvent
    data object OpenDetailStatus : InOperationMinimalEvent
    data object EndOperation : InOperationMinimalEvent
}

class InOperationMinimalViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(InOperationMinimalUiState())
    val uiState: StateFlow<InOperationMinimalUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<InOperationMinimalEvent>()
    val events: SharedFlow<InOperationMinimalEvent> = _events.asSharedFlow()

    private var baseInfo: InOperationMinimalInfo =
        MockInOperationMinimal.forOperationId(OperationRuntimeStateHolder.resolveFocusedOperationId())
    private var refreshJob: Job? = null

    fun load(operationId: String) {
        baseInfo = MockInOperationMinimal.forOperationId(operationId)
        refreshLive()
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            launch {
                LatestLocationHolder.latestFlow.collect { refreshLive() }
            }
            while (isActive) {
                delay(1_000)
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
                    elapsedMinutes = OperationTripClock.elapsedMinutes(start),
                    actualStartTime = OperationTripClock.formatHm(start),
                    lastTransmissionLabel = snap.lastTransmissionLabel,
                    locationStatusLabel = snap.locationOkLabel,
                    transmissionOk = snap.isOk,
                ),
            )
        }
    }

    fun onBack() {
        viewModelScope.launch { _events.emit(InOperationMinimalEvent.NavigateBack) }
    }

    fun onHome() {
        viewModelScope.launch { _events.emit(InOperationMinimalEvent.GoHome) }
    }

    fun onOpenDetailStatus() {
        viewModelScope.launch { _events.emit(InOperationMinimalEvent.OpenDetailStatus) }
    }

    fun onEndOperation() {
        viewModelScope.launch { _events.emit(InOperationMinimalEvent.EndOperation) }
    }
}
