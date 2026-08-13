package com.mju.onda.driver.feature.assignment.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.feature.alarm.data.MockOperationAlarms
import com.mju.onda.driver.feature.assignment.data.AssignmentChangeInfo
import com.mju.onda.driver.feature.home.data.OperationNoticeMapper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AssignmentChangeUiState(
    val info: AssignmentChangeInfo = OperationNoticeMapper.assignmentInfo(""),
    val operationId: String = "",
    val hasUnreadAlarm: Boolean = MockOperationAlarms.hasUnread(),
)

sealed interface AssignmentChangeEvent {
    data class NavigateToVehicleChange(val operationId: String) : AssignmentChangeEvent
    data object NavigateToHome : AssignmentChangeEvent
    data object NavigateBack : AssignmentChangeEvent
    data object OpenAlarms : AssignmentChangeEvent
    data object OpenHistory : AssignmentChangeEvent
    data object OpenSettings : AssignmentChangeEvent
}

class AssignmentChangeViewModel(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val operationId: String =
        savedStateHandle.get<String>("operationId").orEmpty()

    private val _uiState = MutableStateFlow(
        AssignmentChangeUiState(
            info = OperationNoticeMapper.assignmentInfo(operationId),
            operationId = OperationNoticeMapper.operationIdFrom(operationId),
            hasUnreadAlarm = MockOperationAlarms.hasUnread(),
        ),
    )
    val uiState: StateFlow<AssignmentChangeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AssignmentChangeEvent>()
    val events: SharedFlow<AssignmentChangeEvent> = _events.asSharedFlow()

    fun onConfirm() {
        viewModelScope.launch {
            _events.emit(AssignmentChangeEvent.NavigateToVehicleChange(_uiState.value.operationId))
        }
    }

    fun onAlarmClick() {
        viewModelScope.launch { _events.emit(AssignmentChangeEvent.OpenAlarms) }
    }

    fun onBack() {
        viewModelScope.launch { _events.emit(AssignmentChangeEvent.NavigateBack) }
    }

    fun onHistoryClick() {
        viewModelScope.launch { _events.emit(AssignmentChangeEvent.OpenHistory) }
    }

    fun onSettingsClick() {
        viewModelScope.launch { _events.emit(AssignmentChangeEvent.OpenSettings) }
    }

    fun onTodayClick() {
        viewModelScope.launch { _events.emit(AssignmentChangeEvent.NavigateToHome) }
    }
}
