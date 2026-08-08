package com.mju.onda.driver.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
)

sealed interface StopApprovedEvent {
    data object NavigateBack : StopApprovedEvent
    data object EndOperation : StopApprovedEvent
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
        _uiState.update {
            it.copy(
                approvedAt = addMinutes(requestedAt, 6),
                reason = item?.reason ?: "차량 고장",
                actionsEnabled = item?.reviewStatus != SafeStopReviewStatus.ActionCompleted,
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
        SafeStopHistoryHolder.markSelectedActionCompleted()
        _uiState.update { it.copy(actionsEnabled = false) }
        viewModelScope.launch { _events.emit(StopApprovedEvent.EndOperation) }
    }

    fun onContactAdmin() {
        viewModelScope.launch { _events.emit(StopApprovedEvent.ContactAdmin) }
    }

    fun onBack() {
        viewModelScope.launch { _events.emit(StopApprovedEvent.NavigateBack) }
    }
}
