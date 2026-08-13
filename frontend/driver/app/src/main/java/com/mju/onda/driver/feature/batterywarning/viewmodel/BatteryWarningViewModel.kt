package com.mju.onda.driver.feature.batterywarning.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.feature.batterywarning.data.BatteryWarningInfo
import com.mju.onda.driver.feature.batterywarning.data.MockBatteryWarning
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BatteryWarningUiState(
    val info: BatteryWarningInfo = BatteryWarningInfo(
        routeName = "",
        vehicleName = "",
        items = emptyList(),
    ),
    val isConfirming: Boolean = false,
)

sealed interface BatteryWarningEvent {
    data object NavigateBack : BatteryWarningEvent
    data object ShowStillIssues : BatteryWarningEvent
    data object ShowResolved : BatteryWarningEvent
}

class BatteryWarningViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(BatteryWarningUiState())
    val uiState: StateFlow<BatteryWarningUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<BatteryWarningEvent>()
    val events: SharedFlow<BatteryWarningEvent> = _events.asSharedFlow()

    fun refresh() {
        _uiState.update {
            it.copy(info = MockBatteryWarning.forFocusedOperation(getApplication()))
        }
    }

    fun onBack() {
        viewModelScope.launch { _events.emit(BatteryWarningEvent.NavigateBack) }
    }

    /** 실제 기기 충전·절전 상태를 다시 읽는다. */
    fun onConfirmCharger() {
        if (_uiState.value.isConfirming) return
        viewModelScope.launch {
            _uiState.update { it.copy(isConfirming = true) }
            delay(700)
            val info = MockBatteryWarning.forFocusedOperation(getApplication())
            _uiState.update { it.copy(isConfirming = false, info = info) }
            if (info.isAttentionNeeded) {
                _events.emit(BatteryWarningEvent.ShowStillIssues)
            } else {
                _events.emit(BatteryWarningEvent.ShowResolved)
            }
        }
    }

    fun onClose() {
        viewModelScope.launch { _events.emit(BatteryWarningEvent.NavigateBack) }
    }
}
