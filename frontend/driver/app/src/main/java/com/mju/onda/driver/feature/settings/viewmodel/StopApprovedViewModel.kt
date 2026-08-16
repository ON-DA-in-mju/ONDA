package com.mju.onda.driver.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder
import com.mju.onda.driver.feature.settings.data.SafeStopDispatch
import com.mju.onda.driver.feature.settings.data.SafeStopHistoryHolder
import com.mju.onda.driver.feature.settings.data.SafeStopReviewStatus
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StopApprovedUiState(
    val approvedAt: String = "09:24",
    val reason: String = "차량 고장",
    val actionsEnabled: Boolean = true,
    val operationId: String = "",
)

sealed interface StopApprovedEvent {
    data object NavigateBack : StopApprovedEvent
    data class EndOperation(val operationId: String) : StopApprovedEvent
    data object ContactAdmin : StopApprovedEvent
}

class StopApprovedViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(StopApprovedUiState())
    val uiState: StateFlow<StopApprovedUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<StopApprovedEvent>()
    val events: SharedFlow<StopApprovedEvent> = _events.asSharedFlow()

    fun load() {
        val item = SafeStopHistoryHolder.selected()
        val requestedAt = item?.requestedAt ?: "09:18"
        val operationId = SafeStopDispatch.resolvedOperationId(item)
        val live = SafeStopDispatch.isLive(item)
        _uiState.update {
            it.copy(
                approvedAt = addMinutes(requestedAt, 6),
                reason = item?.reason ?: "차량 고장",
                operationId = operationId,
                actionsEnabled = item?.reviewStatus == SafeStopReviewStatus.Confirmed && live,
            )
        }
    }

    private fun addMinutes(time: String, minutes: Int): String {
        val parts = time.split(":")
        if (parts.size != 2) return time
        val hour = parts[0].toIntOrNull() ?: return time
        val minute = parts[1].toIntOrNull() ?: return time
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            add(Calendar.MINUTE, minutes)
        }
        return String.format(
            Locale.KOREA,
            "%02d:%02d",
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
        )
    }

    fun onEndOperation() {
        if (!_uiState.value.actionsEnabled) return
        val operationId = _uiState.value.operationId
        if (!OperationRuntimeStateHolder.isLiveOperation(operationId)) return
        viewModelScope.launch { _events.emit(StopApprovedEvent.EndOperation(operationId)) }
    }

    fun onContactAdmin() {
        viewModelScope.launch { _events.emit(StopApprovedEvent.ContactAdmin) }
    }

    fun onBack() {
        viewModelScope.launch { _events.emit(StopApprovedEvent.NavigateBack) }
    }
}
