package com.mju.onda.driver.feature.alarm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.feature.alarm.data.AlarmCategory
import com.mju.onda.driver.feature.alarm.data.AlarmFilter
import com.mju.onda.driver.feature.alarm.data.AlarmGenerator
import com.mju.onda.driver.feature.alarm.data.AlarmReadStateHolder
import com.mju.onda.driver.feature.alarm.data.DriverNoticesApi
import com.mju.onda.driver.feature.alarm.data.DriverNoticesPoller
import com.mju.onda.driver.feature.alarm.data.MockOperationAlarms
import com.mju.onda.driver.feature.alarm.data.OperationAlarm
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OperationAlarmUiState(
    val selectedFilter: AlarmFilter = AlarmFilter.All,
    val items: List<OperationAlarm> = MockOperationAlarms.filtered(AlarmFilter.All),
    val hasUnread: Boolean = MockOperationAlarms.hasUnread(),
)

sealed interface OperationAlarmEvent {
    data object NavigateBack : OperationAlarmEvent
    data class OpenDetail(val alarmId: String) : OperationAlarmEvent
    data class OpenNoticeDetail(
        val typeLabel: String,
        val headline: String,
        val body: String,
        val dateTime: String,
        val urgent: Boolean,
    ) : OperationAlarmEvent
    data class OpenAssignmentChange(val alarmId: String) : OperationAlarmEvent
    data class OpenDepartureTimeChange(val alarmId: String) : OperationAlarmEvent
    data class OpenOperationCancel(val alarmId: String) : OperationAlarmEvent
    data object OpenHistory : OperationAlarmEvent
    data object OpenSettings : OperationAlarmEvent
}

class OperationAlarmViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(OperationAlarmUiState())
    val uiState: StateFlow<OperationAlarmUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<OperationAlarmEvent>()
    val events: SharedFlow<OperationAlarmEvent> = _events.asSharedFlow()

    fun refresh() {
        viewModelScope.launch {
            DriverNoticesPoller.pollOnce()
            val filter = _uiState.value.selectedFilter
            _uiState.update {
                it.copy(
                    items = MockOperationAlarms.filtered(filter),
                    hasUnread = MockOperationAlarms.hasUnread(),
                )
            }
        }
    }

    fun onFilterSelected(filter: AlarmFilter) {
        _uiState.update {
            it.copy(
                selectedFilter = filter,
                items = MockOperationAlarms.filtered(filter),
                hasUnread = MockOperationAlarms.hasUnread(),
            )
        }
    }

    fun onBack() {
        viewModelScope.launch { _events.emit(OperationAlarmEvent.NavigateBack) }
    }

    fun onItemClick(alarmId: String) {
        if (AlarmGenerator.isBannerAlarmId(alarmId)) return
        AlarmReadStateHolder.markRead(alarmId)
        refresh()
        val alarm = MockOperationAlarms.seedItems.find { it.id == alarmId }
        viewModelScope.launch {
            when (alarm?.category) {
                AlarmCategory.Notice ->
                    _events.emit(
                        OperationAlarmEvent.OpenNoticeDetail(
                            typeLabel = alarm.title,
                            headline = alarm.noticeHeadline ?: alarm.title,
                            body = alarm.noticeContent ?: alarm.body,
                            dateTime = alarm.noticeDateTime ?: alarm.timeLabel,
                            urgent = DriverNoticesApi.isUrgentType(alarm.noticeType.orEmpty()),
                        ),
                    )
                AlarmCategory.AssignmentChange ->
                    _events.emit(OperationAlarmEvent.OpenAssignmentChange(alarmId))
                AlarmCategory.DepartureTimeChange ->
                    _events.emit(OperationAlarmEvent.OpenDepartureTimeChange(alarmId))
                AlarmCategory.OperationCancel ->
                    _events.emit(OperationAlarmEvent.OpenOperationCancel(alarmId))
                else ->
                    _events.emit(OperationAlarmEvent.OpenDetail(alarmId))
            }
        }
    }

    fun onHistoryClick() {
        viewModelScope.launch { _events.emit(OperationAlarmEvent.OpenHistory) }
    }

    fun onSettingsClick() {
        viewModelScope.launch { _events.emit(OperationAlarmEvent.OpenSettings) }
    }
}
