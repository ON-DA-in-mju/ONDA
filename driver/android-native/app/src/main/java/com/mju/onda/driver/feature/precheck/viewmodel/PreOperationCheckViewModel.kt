package com.mju.onda.driver.feature.precheck.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.feature.precheck.data.CheckStatus
import com.mju.onda.driver.feature.precheck.data.PreCheckDeviceStatus
import com.mju.onda.driver.feature.precheck.data.PreCheckItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PreOperationCheckUiState(
    val items: List<PreCheckItem> = emptyList(),
    val isRechecking: Boolean = false,
) {
    /** 조치 필요 항목이 없어야 시작 가능 (주의는 시연상 허용) */
    val canStart: Boolean
        get() = items.isNotEmpty() && items.none { it.status == CheckStatus.ActionRequired }
}

sealed interface PreOperationCheckEvent {
    data object NavigateBack : PreOperationCheckEvent
    data object OpenAppSettings : PreOperationCheckEvent
    data object OpenLocationSettings : PreOperationCheckEvent
    data object NavigateToComplete : PreOperationCheckEvent
    data object ShowStillHasIssues : PreOperationCheckEvent
}

class PreOperationCheckViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PreOperationCheckUiState())
    val uiState: StateFlow<PreOperationCheckUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PreOperationCheckEvent>()
    val events: SharedFlow<PreOperationCheckEvent> = _events.asSharedFlow()

    fun refreshFromSystem(context: Context) {
        _uiState.update {
            it.copy(items = PreCheckDeviceStatus.buildItems(context))
        }
    }

    fun onBack() {
        viewModelScope.launch { _events.emit(PreOperationCheckEvent.NavigateBack) }
    }

    /** 다시 점검: 기기 상태만 다시 읽고, 이 화면에 머무른다. */
    fun onRecheck(context: Context) {
        if (_uiState.value.isRechecking) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRechecking = true) }
            delay(500)
            refreshFromSystem(context)
            delay(300)
            _uiState.update { it.copy(isRechecking = false) }
            if (!_uiState.value.canStart) {
                _events.emit(PreOperationCheckEvent.ShowStillHasIssues)
            }
        }
    }

    fun onOpenSettings() {
        viewModelScope.launch {
            val gpsOff = _uiState.value.items.any {
                it.id == "gps" && it.status == CheckStatus.ActionRequired
            }
            if (gpsOff) {
                _events.emit(PreOperationCheckEvent.OpenLocationSettings)
            } else {
                _events.emit(PreOperationCheckEvent.OpenAppSettings)
            }
        }
    }

    /** 모두 정상일 때만 다음 화면(점검 완료)으로 이동 */
    fun onStartOperation() {
        if (!_uiState.value.canStart) return
        viewModelScope.launch { _events.emit(PreOperationCheckEvent.NavigateToComplete) }
    }
}
