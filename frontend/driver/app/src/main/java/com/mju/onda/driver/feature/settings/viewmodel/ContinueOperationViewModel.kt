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

data class ContinueOperationUiState(
    val noticeAt: String = "09:22",
    val reason: String = "차량 고장",
    val actionsEnabled: Boolean = true,
    val operationId: String = "",
)

sealed interface ContinueOperationEvent {
    data object NavigateBack : ContinueOperationEvent
    data class Continue(val operationId: String) : ContinueOperationEvent
    data object ContactAdmin : ContinueOperationEvent
}

class ContinueOperationViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ContinueOperationUiState())
    val uiState: StateFlow<ContinueOperationUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ContinueOperationEvent>()
    val events: SharedFlow<ContinueOperationEvent> = _events.asSharedFlow()

    fun load() {
        val item = SafeStopHistoryHolder.selected()
        val requestedAt = item?.requestedAt ?: "09:18"
        val operationId = SafeStopDispatch.resolvedOperationId(item)
        val live = SafeStopDispatch.isLive(item)
        _uiState.update {
            it.copy(
                noticeAt = addMinutes(requestedAt, 4),
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

    fun onContinue() {
        if (!_uiState.value.actionsEnabled) return
        val operationId = _uiState.value.operationId
        if (!OperationRuntimeStateHolder.isLiveOperation(operationId)) return
        SafeStopHistoryHolder.markSelectedActionCompleted()
        _uiState.update { it.copy(actionsEnabled = false) }
        viewModelScope.launch { _events.emit(ContinueOperationEvent.Continue(operationId)) }
    }

    fun onContactAdmin() {
        viewModelScope.launch { _events.emit(ContinueOperationEvent.ContactAdmin) }
    }

    fun onBack() {
        viewModelScope.launch { _events.emit(ContinueOperationEvent.NavigateBack) }
    }
}
