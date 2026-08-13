package com.mju.onda.driver.feature.settings.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.core.location.LatestLocationHolder
import com.mju.onda.driver.core.location.OperationDeviceStatus
import com.mju.onda.driver.feature.home.data.MockTodayOperations
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder
import com.mju.onda.driver.feature.operation.data.MockOperationDetail
import com.mju.onda.driver.feature.settings.data.MockLogoutRestricted
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

data class LogoutRestrictedUiState(
    val operationId: String = "",
    val routeName: String = "",
    val vehicleName: String = "",
    val statusLabel: String = MockLogoutRestricted.STATUS_IN_PROGRESS,
    val locationStatus: String = MockLogoutRestricted.LOCATION_OK,
    val transmissionOk: Boolean = false,
)

sealed interface LogoutRestrictedEvent {
    data object NavigateBack : LogoutRestrictedEvent
    data class GoToOperation(val operationId: String) : LogoutRestrictedEvent
    data object ContactAdmin : LogoutRestrictedEvent
    data object ClearedStaleForLogout : LogoutRestrictedEvent
}

class LogoutRestrictedViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(LogoutRestrictedUiState())
    val uiState: StateFlow<LogoutRestrictedUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LogoutRestrictedEvent>()
    val events: SharedFlow<LogoutRestrictedEvent> = _events.asSharedFlow()

    private var refreshJob: Job? = null

    fun load() {
        val operationId = OperationRuntimeStateHolder.resolveFocusedOperationId()
        val detail = MockOperationDetail.forOperationId(operationId)
        val op = MockTodayOperations.findById(operationId)
        _uiState.value = LogoutRestrictedUiState(
            operationId = operationId,
            routeName = op?.routeName ?: detail.routeName,
            vehicleName = op?.vehicleName ?: detail.vehicleName,
            statusLabel = if (OperationRuntimeStateHolder.isInProgress(operationId)) {
                MockLogoutRestricted.STATUS_IN_PROGRESS
            } else {
                MockTodayOperations.statusLabel(
                    op?.status ?: detail.status,
                )
            },
        )
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
        val operationId = _uiState.value.operationId
        val snap = OperationDeviceStatus.transmissionSnapshot(
            getApplication(),
            operationId.takeIf { it.isNotBlank() },
        )
        _uiState.update {
            it.copy(
                locationStatus = snap.shortStatusLabel,
                transmissionOk = snap.isOk,
            )
        }
    }

    fun onGoToOperation() {
        val id = _uiState.value.operationId
        if (id.isBlank()) return
        viewModelScope.launch {
            _events.emit(LogoutRestrictedEvent.GoToOperation(id))
        }
    }

    fun onContactAdmin() {
        viewModelScope.launch { _events.emit(LogoutRestrictedEvent.ContactAdmin) }
    }

    /** 배차 목록과 어긋난 로컬 운행중 플래그 제거 → 로그아웃 확인으로 */
    fun onClearStaleAndLogout() {
        OperationRuntimeStateHolder.clearOrphanedActiveOperations()
        // 그래도 남아 있으면(목록에 실제 운행중이 있음) 강제 종료는 하지 않음
        if (OperationRuntimeStateHolder.hasActiveOperation()) {
            val ops = MockTodayOperations.assignedOperations
            val active = OperationRuntimeStateHolder.activeOperationId()
            val stillReal = ops.any { it.id == active && it.status == com.mju.onda.driver.feature.home.data.OperationStatus.InProgress }
            if (!stillReal) {
                OperationRuntimeStateHolder.clearAll()
            }
        }
        viewModelScope.launch {
            if (OperationRuntimeStateHolder.hasActiveOperation()) {
                // 실제 운행 중이면 안내만 갱신
                load()
            } else {
                _events.emit(LogoutRestrictedEvent.ClearedStaleForLogout)
            }
        }
    }

    fun onBack() {
        viewModelScope.launch { _events.emit(LogoutRestrictedEvent.NavigateBack) }
    }
}
