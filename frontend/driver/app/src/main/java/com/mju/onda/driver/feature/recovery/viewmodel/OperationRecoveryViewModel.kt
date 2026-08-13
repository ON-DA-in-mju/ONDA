package com.mju.onda.driver.feature.recovery.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.core.location.LatestLocationHolder
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder
import com.mju.onda.driver.feature.recovery.data.MockOperationRecovery
import com.mju.onda.driver.feature.recovery.data.OperationRecoveryInfo
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

data class OperationRecoveryUiState(
    val info: OperationRecoveryInfo? = null,
    val showBackgroundInfoBanner: Boolean = true,
    val showBatteryWarningBanner: Boolean = false,
)

sealed interface OperationRecoveryEvent {
    data object NavigateBack : OperationRecoveryEvent
    data class GoToOperation(val operationId: String) : OperationRecoveryEvent
    data object GoToTodayOperation : OperationRecoveryEvent
    data object OpenBackgroundGuide : OperationRecoveryEvent
    data object OpenBatteryWarning : OperationRecoveryEvent
}

class OperationRecoveryViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(OperationRecoveryUiState())
    val uiState: StateFlow<OperationRecoveryUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<OperationRecoveryEvent>()
    val events: SharedFlow<OperationRecoveryEvent> = _events.asSharedFlow()

    private var refreshJob: Job? = null

    init {
        refreshFromDevice()
        refreshJob = viewModelScope.launch {
            launch {
                LatestLocationHolder.latestFlow.collect { refreshFromDevice() }
            }
            while (isActive) {
                delay(5_000)
                refreshFromDevice()
            }
        }
    }

    fun refreshBannerVisibility() {
        refreshFromDevice()
    }

    private fun refreshFromDevice() {
        val operationId = OperationRuntimeStateHolder.resolveFocusedOperationId()
        val info = MockOperationRecovery.forOperationId(getApplication(), operationId)
        _uiState.update {
            it.copy(
                info = info,
                showBackgroundInfoBanner = true,
                showBatteryWarningBanner = info.showBatteryWarning,
            )
        }
    }

    fun onBack() {
        viewModelScope.launch { _events.emit(OperationRecoveryEvent.NavigateBack) }
    }

    fun onGoToOperation() {
        val operationId = _uiState.value.info?.id
            ?: OperationRuntimeStateHolder.resolveFocusedOperationId()
        viewModelScope.launch {
            _events.emit(OperationRecoveryEvent.GoToOperation(operationId))
        }
    }

    fun onGoToToday() {
        viewModelScope.launch { _events.emit(OperationRecoveryEvent.GoToTodayOperation) }
    }

    fun onBackgroundInfoClick() {
        viewModelScope.launch {
            _events.emit(OperationRecoveryEvent.OpenBackgroundGuide)
        }
    }

    fun onBatteryWarningClick() {
        viewModelScope.launch {
            _events.emit(OperationRecoveryEvent.OpenBatteryWarning)
        }
    }
}
