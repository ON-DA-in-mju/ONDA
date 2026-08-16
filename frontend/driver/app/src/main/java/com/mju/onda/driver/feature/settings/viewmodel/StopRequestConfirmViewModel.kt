package com.mju.onda.driver.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.feature.auth.data.SessionStateHolder
import com.mju.onda.driver.feature.home.data.MockTodayOperations
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder
import com.mju.onda.driver.feature.settings.data.AccountInfoStateHolder
import com.mju.onda.driver.feature.settings.data.MockStopRequestConfirm
import com.mju.onda.driver.feature.settings.data.MockSafeStopHistory
import com.mju.onda.driver.feature.settings.data.SafeStopApi
import com.mju.onda.driver.feature.settings.data.SafeStopHistoryHolder
import com.mju.onda.driver.feature.settings.data.SafeStopHistoryItem
import com.mju.onda.driver.feature.settings.data.StopRequestDraft
import com.mju.onda.driver.feature.settings.data.StopRequestDraftHolder
import com.mju.onda.driver.feature.settings.data.StopRequestReceivedHolder
import com.mju.onda.driver.feature.settings.data.StopRequestReceivedInfo
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
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
    val sending: Boolean = false,
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
        if (_uiState.value.sending) return
        viewModelScope.launch {
            _uiState.update { it.copy(sending = true) }
            val draft = StopRequestDraftHolder.draft
            val reason = draft?.reason ?: _uiState.value.reason
            val detailReason = draft?.message.orEmpty().trim()
            val requestedAt = resolveRequestedAt()
            val operationId = OperationRuntimeStateHolder.activeOperationId()
                ?: MockTodayOperations.assignedOperations.firstOrNull()?.id
                ?: "unknown"
            val operation = MockTodayOperations.assignedOperations
                .find { it.id == operationId }
                ?: MockTodayOperations.assignedOperations.firstOrNull()
            val routeName = draft?.routeName ?: operation?.routeName.orEmpty()
            val vehicleName = draft?.vehicleName
                ?: AccountInfoStateHolder.get().vehicleName.ifBlank { operation?.vehicleName.orEmpty() }
            val requestId = "stop-${System.currentTimeMillis()}"
            val driverId = SessionStateHolder.currentUserId.orEmpty()
            val driverName = stripHonorific(AccountInfoStateHolder.get().driverName)

            // 로컬에는 항상 Pending으로 남기고, 관리자 결정은 새로고침으로만 반영
            SafeStopHistoryHolder.add(
                SafeStopHistoryItem(
                    id = requestId,
                    reason = reason,
                    requestedAt = requestedAt,
                    routeName = routeName,
                    vehicleName = vehicleName,
                    dateLabel = MockSafeStopHistory.TODAY_DATE_LABEL,
                    operationId = operationId.takeIf { it != "unknown" }.orEmpty(),
                ),
            )
            StopRequestReceivedHolder.set(
                StopRequestReceivedInfo(
                    requestId = requestId,
                    reason = reason,
                    requestedAt = requestedAt,
                ),
            )
            SafeStopHistoryHolder.select(requestId)

            if (driverId.isNotBlank()) {
                when (
                    val posted = SafeStopApi.postRequest(
                        id = requestId,
                        driverId = driverId,
                        driverName = driverName.ifBlank { driverId },
                        vehicleName = vehicleName.ifBlank { "미정" },
                        routeName = routeName.ifBlank { "-" },
                        operationId = operationId,
                        reason = reason,
                        detailReason = detailReason,
                        requestedAt = requestedAt,
                        date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                    )
                ) {
                    is SafeStopApi.PostResult.Ok -> {
                        val serverId = posted.id
                        if (serverId != requestId) {
                            SafeStopHistoryHolder.remapId(requestId, serverId)
                            StopRequestReceivedHolder.set(
                                StopRequestReceivedInfo(
                                    requestId = serverId,
                                    reason = reason,
                                    requestedAt = requestedAt,
                                ),
                            )
                            SafeStopHistoryHolder.select(serverId)
                        }
                    }
                    SafeStopApi.PostResult.Failed -> Unit
                }
            }

            StopRequestDraftHolder.clear()
            _uiState.update { it.copy(sending = false) }
            _events.emit(StopRequestConfirmEvent.Sent)
        }
    }

    /** 실제 요청 시각(현재 시각) */
    private fun resolveRequestedAt(): String {
        val now = LocalTime.now()
        return String.format(Locale.KOREA, "%02d:%02d", now.hour, now.minute)
    }

    private fun stripHonorific(name: String): String =
        name.replace(" 기사님", "").replace("기사님", "").trim()

    fun onCancel() {
        viewModelScope.launch { _events.emit(StopRequestConfirmEvent.NavigateBackToDetail) }
    }

    fun onBack() {
        viewModelScope.launch { _events.emit(StopRequestConfirmEvent.NavigateBackToDetail) }
    }
}
