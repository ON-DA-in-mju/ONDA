package com.mju.onda.driver.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder
import com.mju.onda.driver.feature.settings.data.SafeStopHistoryHolder
import com.mju.onda.driver.feature.settings.data.SafeStopHistoryItem
import com.mju.onda.driver.feature.settings.data.SafeStopOutcome
import com.mju.onda.driver.feature.settings.data.SafeStopReviewStatus
import com.mju.onda.driver.feature.settings.data.StopRequestReceivedHolder
import com.mju.onda.driver.feature.settings.data.StopRequestReceivedInfo
import kotlin.random.Random
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

    fun load() {
        SafeStopHistoryHolder.ensureSeedIfEmpty()
        refreshState()
    }

    private fun refreshState() {
        _uiState.update {
            it.copy(
                items = SafeStopHistoryHolder.all(),
                canCreateRequest = OperationRuntimeStateHolder.hasActiveOperation(),
            )
        }
    }

    fun onRefresh() {
        SafeStopHistoryHolder.markAllConfirmed {
            if (Random.nextBoolean()) SafeStopOutcome.Approved else SafeStopOutcome.ContinueOperation
        }
        refreshState()
        viewModelScope.launch { _events.emit(SafeStopHistoryEvent.Refreshed) }
    }

    fun onItemClick(item: SafeStopHistoryItem) {
        SafeStopHistoryHolder.select(item.id)
        viewModelScope.launch {
            when (item.reviewStatus) {
                SafeStopReviewStatus.Pending -> {
                    StopRequestReceivedHolder.set(
                        StopRequestReceivedInfo(
                            reason = item.reason,
                            requestedAt = item.requestedAt,
                        ),
                    )
                    _events.emit(SafeStopHistoryEvent.OpenReceived(item.id))
                }
                SafeStopReviewStatus.Confirmed,
                SafeStopReviewStatus.ActionCompleted,
                -> {
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
}
