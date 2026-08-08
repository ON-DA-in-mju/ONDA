package com.mju.onda.driver.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.feature.settings.data.MockStopRequestReceived
import com.mju.onda.driver.feature.settings.data.StopRequestReceivedHolder
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StopRequestReceivedUiState(
    val reason: String = MockStopRequestReceived.FALLBACK_REASON,
    val requestedAt: String = MockStopRequestReceived.FALLBACK_TIME,
    val adminStatus: String = MockStopRequestReceived.ADMIN_PENDING,
    val gpsStatus: String = MockStopRequestReceived.GPS_SENDING,
)

sealed interface StopRequestReceivedEvent {
    data object NavigateBack : StopRequestReceivedEvent
    data object GoToList : StopRequestReceivedEvent
    data object ContactAdmin : StopRequestReceivedEvent
}

class StopRequestReceivedViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(StopRequestReceivedUiState())
    val uiState: StateFlow<StopRequestReceivedUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<StopRequestReceivedEvent>()
    val events: SharedFlow<StopRequestReceivedEvent> = _events.asSharedFlow()

    fun load() {
        val info = StopRequestReceivedHolder.info
        _uiState.update {
            it.copy(
                reason = info?.reason?.ifBlank { MockStopRequestReceived.FALLBACK_REASON }
                    ?: MockStopRequestReceived.FALLBACK_REASON,
                requestedAt = info?.requestedAt?.ifBlank { MockStopRequestReceived.FALLBACK_TIME }
                    ?: MockStopRequestReceived.FALLBACK_TIME,
            )
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
}
