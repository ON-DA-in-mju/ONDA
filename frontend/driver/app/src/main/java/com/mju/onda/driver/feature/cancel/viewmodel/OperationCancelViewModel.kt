package com.mju.onda.driver.feature.cancel.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.feature.alarm.data.MockOperationAlarms
import com.mju.onda.driver.feature.cancel.data.MockOperationCancel
import com.mju.onda.driver.feature.cancel.data.OperationCancelInfo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OperationCancelUiState(
    val info: OperationCancelInfo = MockOperationCancel.info,
    val hasUnreadAlarm: Boolean = MockOperationAlarms.hasUnread(),
)

sealed interface OperationCancelEvent {
    data object ConfirmAndGoHome : OperationCancelEvent
    data object NavigateBack : OperationCancelEvent
    data object OpenAlarms : OperationCancelEvent
    data object OpenHistory : OperationCancelEvent
    data object OpenSettings : OperationCancelEvent
}

class OperationCancelViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(OperationCancelUiState())
    val uiState: StateFlow<OperationCancelUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<OperationCancelEvent>()
    val events: SharedFlow<OperationCancelEvent> = _events.asSharedFlow()

    fun onConfirm() {
        viewModelScope.launch {
            _events.emit(OperationCancelEvent.ConfirmAndGoHome)
        }
    }

    fun onBack() {
        viewModelScope.launch {
            _events.emit(OperationCancelEvent.NavigateBack)
        }
    }

    fun onAlarmClick() {
        viewModelScope.launch {
            _events.emit(OperationCancelEvent.OpenAlarms)
        }
    }

    fun onHistoryClick() {
        viewModelScope.launch {
            _events.emit(OperationCancelEvent.OpenHistory)
        }
    }

    fun onSettingsClick() {
        viewModelScope.launch {
            _events.emit(OperationCancelEvent.OpenSettings)
        }
    }

    fun onTodayClick() {
        viewModelScope.launch {
            _events.emit(OperationCancelEvent.ConfirmAndGoHome)
        }
    }
}
