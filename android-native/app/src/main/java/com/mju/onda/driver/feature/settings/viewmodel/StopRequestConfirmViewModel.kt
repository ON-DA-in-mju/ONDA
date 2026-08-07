package com.mju.onda.driver.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.feature.home.data.MockTodayOperations
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder
import com.mju.onda.driver.feature.settings.data.MockStopRequestConfirm
import com.mju.onda.driver.feature.settings.data.MockStopRequestReceived
import com.mju.onda.driver.feature.settings.data.MockSafeStopHistory
import com.mju.onda.driver.feature.settings.data.SafeStopHistoryHolder
import com.mju.onda.driver.feature.settings.data.SafeStopHistoryItem
import com.mju.onda.driver.feature.settings.data.StopRequestDraft
import com.mju.onda.driver.feature.settings.data.StopRequestDraftHolder
import com.mju.onda.driver.feature.settings.data.StopRequestReceivedHolder
import com.mju.onda.driver.feature.settings.data.StopRequestReceivedInfo
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StopRequestConfirmUiState(
    val reason: String = "",
    val routeName: String = "",
    val vehicleName: String = "",
    val locationLabel: String = "",
    val attachmentLabel: String = "",
    val includeLocation: Boolean = true,
)

sealed interface StopRequestConfirmEvent {
    data object NavigateBackToDetail : StopRequestConfirmEvent
    data object Sent : StopRequestConfirmEvent
}

class StopRequestConfirmViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(StopRequestConfirmUiState())
    val uiState: StateFlow<StopRequestConfirmUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<StopRequestConfirmEvent>()
    val events: SharedFlow<StopRequestConfirmEvent> = _events.asSharedFlow()

    fun load() {
        val draft = StopRequestDraftHolder.draft ?: return
        applyDraft(draft)
    }

    private fun applyDraft(draft: StopRequestDraft) {
        _uiState.update {
            it.copy(
                reason = draft.reason,
                routeName = draft.routeName,
                vehicleName = draft.vehicleName,
                locationLabel = if (draft.includeLocation) {
                    draft.locationLabel.ifBlank { MockStopRequestConfirm.LOCATION_FALLBACK }
                } else {
                    MockStopRequestConfirm.LOCATION_NOT_SHARED
                },
                attachmentLabel = if (draft.includeLocation) {
                    MockStopRequestConfirm.ATTACHMENT_WITH_LOCATION
                } else {
                    MockStopRequestConfirm.ATTACHMENT_WITHOUT_LOCATION
                },
                includeLocation = draft.includeLocation,
            )
        }
    }

    fun onSend() {
        viewModelScope.launch {
            val draft = StopRequestDraftHolder.draft
            val reason = draft?.reason ?: _uiState.value.reason
            val requestedAt = resolveRequestedAt()
            val operationId = OperationRuntimeStateHolder.activeOperationId()
            val operation = MockTodayOperations.assignedOperations
                .find { it.id == operationId }
                ?: MockTodayOperations.assignedOperations.firstOrNull()
            StopRequestReceivedHolder.set(
                StopRequestReceivedInfo(
                    reason = reason,
                    requestedAt = requestedAt,
                ),
            )
            SafeStopHistoryHolder.add(
                SafeStopHistoryItem(
                    id = "stop-${System.currentTimeMillis()}",
                    reason = reason,
                    requestedAt = requestedAt,
                    routeName = draft?.routeName ?: operation?.routeName.orEmpty(),
                    vehicleName = draft?.vehicleName ?: operation?.vehicleName.orEmpty(),
                    dateLabel = MockSafeStopHistory.TODAY_DATE_LABEL,
                ),
            )
            StopRequestDraftHolder.clear()
            _events.emit(StopRequestConfirmEvent.Sent)
        }
    }

    /**
     * 운행 시작 시각(배차 departTime) + 1~30분 랜덤.
     * 예: 10:03 시작, 19분 → 10:22
     */
    private fun resolveRequestedAt(): String {
        val operationId = OperationRuntimeStateHolder.activeOperationId()
        val operation = MockTodayOperations.assignedOperations
            .find { it.id == operationId }
            ?: MockTodayOperations.assignedOperations.firstOrNull()
        val base = operation?.departTime ?: MockStopRequestReceived.FALLBACK_TIME
        val parts = base.split(":")
        if (parts.size != 2) return base
        val hour = parts[0].toIntOrNull() ?: return base
        val minute = parts[1].toIntOrNull() ?: return base
        val offsetMinutes = Random.nextInt(1, 31)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, offsetMinutes)
        }
        return String.format(
            Locale.KOREA,
            "%02d:%02d",
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
        )
    }

    fun onCancel() {
        viewModelScope.launch { _events.emit(StopRequestConfirmEvent.NavigateBackToDetail) }
    }

    fun onBack() {
        viewModelScope.launch { _events.emit(StopRequestConfirmEvent.NavigateBackToDetail) }
    }
}
