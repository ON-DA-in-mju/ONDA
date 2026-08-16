package com.mju.onda.driver.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.feature.settings.data.MockStopRequestReceived
import com.mju.onda.driver.feature.settings.data.SafeStopDecisionPoller
import com.mju.onda.driver.feature.settings.data.SafeStopDispatch
import com.mju.onda.driver.feature.settings.data.SafeStopHistoryHolder
import com.mju.onda.driver.feature.settings.data.SafeStopReviewStatus
import com.mju.onda.driver.feature.settings.data.SafeStopApi
import com.mju.onda.driver.feature.settings.data.StopRequestReceivedHolder
import com.mju.onda.driver.feature.settings.data.SafeStopHistoryItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StopRequestReceivedUiState(
    val requestId: String = "",
    val reason: String = MockStopRequestReceived.FALLBACK_REASON,
    val requestedAt: String = MockStopRequestReceived.FALLBACK_TIME,
    val adminStatus: String = MockStopRequestReceived.ADMIN_PENDING,
    val gpsStatus: String = MockStopRequestReceived.GPS_SENDING,
    val canCancel: Boolean = true,
    val cancelling: Boolean = false,
    val showCancelConfirmDialog: Boolean = false,
)

sealed interface StopRequestReceivedEvent {
    data object NavigateBack : StopRequestReceivedEvent
    data object GoHome : StopRequestReceivedEvent
    data object GoToList : StopRequestReceivedEvent
    data object ContactAdmin : StopRequestReceivedEvent
    data object Cancelled : StopRequestReceivedEvent
    data class OpenApproved(val itemId: String) : StopRequestReceivedEvent
    data class OpenContinue(val itemId: String) : StopRequestReceivedEvent
}

class StopRequestReceivedViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(StopRequestReceivedUiState())
    val uiState: StateFlow<StopRequestReceivedUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<StopRequestReceivedEvent>()
    val events: SharedFlow<StopRequestReceivedEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            SafeStopDecisionPoller.updates.collect { update ->
                val currentId = _uiState.value.requestId
                if (currentId.isBlank() || update.requestId != currentId) return@collect
                handleRemoteDecision(update.requestId, update.decision)
            }
        }
    }

    fun load() {
        val info = StopRequestReceivedHolder.info
        val requestId = info?.requestId?.ifBlank { null }
            ?: SafeStopHistoryHolder.selected()?.id.orEmpty()
        val selected = requestId.takeIf { it.isNotBlank() }?.let { id ->
            SafeStopHistoryHolder.all().find { it.id == id }
        } ?: SafeStopHistoryHolder.selected()
        applyItem(requestId.ifBlank { selected?.id.orEmpty() }, selected, info?.reason, info?.requestedAt)

        viewModelScope.launch {
            val updates = SafeStopDecisionPoller.pollOnce()
            val id = _uiState.value.requestId
            val matched = updates.find { it.requestId == id }
            if (matched != null) {
                handleRemoteDecision(matched.requestId, matched.decision)
            } else {
                val refreshed = SafeStopHistoryHolder.all().find { it.id == id }
                applyItem(id, refreshed, null, null)
            }
        }
    }

    private fun applyItem(
        requestId: String,
        selected: SafeStopHistoryItem?,
        reasonFallback: String?,
        timeFallback: String?,
    ) {
        val cancelled = selected?.reviewStatus == SafeStopReviewStatus.Cancelled
        val pending = selected?.reviewStatus == SafeStopReviewStatus.Pending || selected == null
        _uiState.update {
            it.copy(
                requestId = requestId,
                reason = reasonFallback?.ifBlank { null }
                    ?: selected?.reason
                    ?: MockStopRequestReceived.FALLBACK_REASON,
                requestedAt = timeFallback?.ifBlank { null }
                    ?: selected?.requestedAt
                    ?: MockStopRequestReceived.FALLBACK_TIME,
                adminStatus = when {
                    cancelled -> MockStopRequestReceived.ADMIN_CANCELLED
                    selected?.reviewStatus == SafeStopReviewStatus.Confirmed ||
                        selected?.reviewStatus == SafeStopReviewStatus.ActionCompleted ->
                        MockStopRequestReceived.ADMIN_PENDING // 곧 화면 전환
                    else -> MockStopRequestReceived.ADMIN_PENDING
                },
                canCancel = !cancelled && pending &&
                    selected?.reviewStatus != SafeStopReviewStatus.Confirmed &&
                    SafeStopDispatch.isLive(selected),
            )
        }
    }

    private suspend fun handleRemoteDecision(requestId: String, decision: String) {
        SafeStopHistoryHolder.select(requestId)
        when (decision) {
            "cancelled" -> {
                _uiState.update {
                    it.copy(
                        canCancel = false,
                        adminStatus = MockStopRequestReceived.ADMIN_CANCELLED,
                    )
                }
                StopRequestReceivedHolder.clear()
                _events.emit(StopRequestReceivedEvent.Cancelled)
            }
            "stop" -> {
                val item = SafeStopHistoryHolder.all().find { it.id == requestId }
                StopRequestReceivedHolder.clear()
                if (SafeStopDispatch.isLive(item)) {
                    _events.emit(StopRequestReceivedEvent.OpenApproved(requestId))
                } else {
                    _events.emit(StopRequestReceivedEvent.GoToList)
                }
            }
            "continue" -> {
                val item = SafeStopHistoryHolder.all().find { it.id == requestId }
                StopRequestReceivedHolder.clear()
                if (SafeStopDispatch.isLive(item)) {
                    _events.emit(StopRequestReceivedEvent.OpenContinue(requestId))
                } else {
                    _events.emit(StopRequestReceivedEvent.GoToList)
                }
            }
        }
    }

    fun onCancelRequestClick() {
        if (_uiState.value.cancelling || !_uiState.value.canCancel) return
        _uiState.update { it.copy(showCancelConfirmDialog = true) }
    }

    fun dismissCancelConfirmDialog() {
        _uiState.update { it.copy(showCancelConfirmDialog = false) }
    }

    fun onCancelConfirmYes() {
        if (_uiState.value.cancelling || !_uiState.value.canCancel) return
        viewModelScope.launch {
            _uiState.update { it.copy(showCancelConfirmDialog = false, cancelling = true) }
            val id = _uiState.value.requestId.ifBlank {
                SafeStopHistoryHolder.selected()?.id.orEmpty()
            }
            if (id.isNotBlank()) {
                SafeStopHistoryHolder.cancelById(id)
                SafeStopApi.cancelRequest(id)
            }
            _uiState.update {
                it.copy(
                    cancelling = false,
                    canCancel = false,
                    adminStatus = MockStopRequestReceived.ADMIN_CANCELLED,
                )
            }
            StopRequestReceivedHolder.clear()
            _events.emit(StopRequestReceivedEvent.Cancelled)
        }
    }

    fun onGoToList() {
        viewModelScope.launch {
            StopRequestReceivedHolder.clear()
            _events.emit(StopRequestReceivedEvent.GoToList)
        }
    }

    fun onContactAdmin() {
        viewModelScope.launch { _events.emit(StopRequestReceivedEvent.ContactAdmin) }
    }

    fun onBack() {
        viewModelScope.launch { _events.emit(StopRequestReceivedEvent.NavigateBack) }
    }

    fun onHome() {
        viewModelScope.launch { _events.emit(StopRequestReceivedEvent.GoHome) }
    }
}
