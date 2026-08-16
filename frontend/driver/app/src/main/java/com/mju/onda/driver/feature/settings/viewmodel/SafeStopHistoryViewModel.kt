package com.mju.onda.driver.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.feature.home.data.MockTodayOperations
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder
import com.mju.onda.driver.feature.settings.data.MockSafeStopHistory
import com.mju.onda.driver.feature.settings.data.SafeStopDecisionPoller
import com.mju.onda.driver.feature.settings.data.SafeStopDispatch
import com.mju.onda.driver.feature.settings.data.SafeStopHistoryHolder
import com.mju.onda.driver.feature.settings.data.SafeStopHistoryItem
import com.mju.onda.driver.feature.settings.data.SafeStopOutcome
import com.mju.onda.driver.feature.settings.data.SafeStopReviewStatus
import com.mju.onda.driver.feature.settings.data.StopRequestReceivedHolder
import com.mju.onda.driver.feature.settings.data.StopRequestReceivedInfo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SafeStopHistoryUiState(
    val items: List<SafeStopHistoryItem> = emptyList(),
    val canCreateRequest: Boolean = false,
)

sealed interface SafeStopHistoryEvent {
    data object NavigateBack : SafeStopHistoryEvent
    data object GoHome : SafeStopHistoryEvent
    data object OpenNewRequest : SafeStopHistoryEvent
    data object NotInOperation : SafeStopHistoryEvent
    data object Refreshed : SafeStopHistoryEvent
    data class OpenReceived(val itemId: String) : SafeStopHistoryEvent
    data class OpenApproved(val itemId: String) : SafeStopHistoryEvent
    data class OpenContinue(val itemId: String) : SafeStopHistoryEvent
}

class SafeStopHistoryViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SafeStopHistoryUiState())
    val uiState: StateFlow<SafeStopHistoryUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SafeStopHistoryEvent>()
    val events: SharedFlow<SafeStopHistoryEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            SafeStopDecisionPoller.updates.collect {
                refreshState()
            }
        }
    }

    fun load() {
        refreshState()
        viewModelScope.launch {
            syncFromServer()
            refreshState()
        }
    }

    private fun refreshState() {
        reconcileEndedDispatches()
        _uiState.update {
            it.copy(
                items = SafeStopHistoryHolder.all(),
                canCreateRequest = OperationRuntimeStateHolder.hasActiveOperation(),
            )
        }
    }

    private fun reconcileEndedDispatches() {
        MockTodayOperations.assignedOperations
            .filter { OperationRuntimeStateHolder.isEnded(it.id) }
            .forEach { op ->
                SafeStopHistoryHolder.markDispatchEnded(
                    operationId = op.id,
                    routeName = op.routeName,
                    vehicleName = op.vehicleName,
                    dateLabel = MockSafeStopHistory.TODAY_DATE_LABEL,
                )
            }
        SafeStopHistoryHolder.all()
            .filter { it.operationId.isNotBlank() && OperationRuntimeStateHolder.isEnded(it.operationId) }
            .forEach { item ->
                SafeStopHistoryHolder.markDispatchEnded(
                    operationId = item.operationId,
                    routeName = item.routeName,
                    vehicleName = item.vehicleName,
                    dateLabel = item.dateLabel,
                )
            }
    }

    private suspend fun syncFromServer() {
        val key = com.mju.onda.driver.core.supabase.SupabaseClient.userUuid
            ?: com.mju.onda.driver.feature.auth.data.SessionStateHolder.currentUserId
            ?: return
        when (val result = com.mju.onda.driver.feature.settings.data.SafeStopApi.fetchForDriver(key)) {
            is com.mju.onda.driver.feature.settings.data.SafeStopApi.FetchResult.Ok -> {
                SafeStopHistoryHolder.mergeRemote(result.items)
                SafeStopDecisionPoller.pollOnce()
            }
            com.mju.onda.driver.feature.settings.data.SafeStopApi.FetchResult.Failed -> {
                SafeStopDecisionPoller.pollOnce()
            }
        }
    }

    fun onRefresh() {
        viewModelScope.launch {
            syncFromServer()
            refreshState()
            _events.emit(SafeStopHistoryEvent.Refreshed)
        }
    }

    fun onItemClick(item: SafeStopHistoryItem) {
        SafeStopHistoryHolder.select(item.id)
        viewModelScope.launch {
            when (item.reviewStatus) {
                SafeStopReviewStatus.Pending -> {
                    if (!SafeStopDispatch.isLive(item)) return@launch
                    StopRequestReceivedHolder.set(
                        StopRequestReceivedInfo(
                            requestId = item.id,
                            reason = item.reason,
                            requestedAt = item.requestedAt,
                        ),
                    )
                    _events.emit(SafeStopHistoryEvent.OpenReceived(item.id))
                }
                SafeStopReviewStatus.Cancelled,
                SafeStopReviewStatus.ActionCompleted,
                -> Unit
                SafeStopReviewStatus.Confirmed -> {
                    if (!SafeStopDispatch.isLive(item)) return@launch
                    when (item.outcome ?: SafeStopOutcome.Approved) {
                        SafeStopOutcome.Approved ->
                            _events.emit(SafeStopHistoryEvent.OpenApproved(item.id))
                        SafeStopOutcome.ContinueOperation ->
                            _events.emit(SafeStopHistoryEvent.OpenContinue(item.id))
                    }
                }
            }
        }
    }

    fun onNewRequest() {
        viewModelScope.launch {
            if (OperationRuntimeStateHolder.hasActiveOperation()) {
                _events.emit(SafeStopHistoryEvent.OpenNewRequest)
            } else {
                _events.emit(SafeStopHistoryEvent.NotInOperation)
            }
        }
    }

    fun onBack() {
        viewModelScope.launch { _events.emit(SafeStopHistoryEvent.NavigateBack) }
    }

    fun onHome() {
        viewModelScope.launch { _events.emit(SafeStopHistoryEvent.GoHome) }
    }
}
