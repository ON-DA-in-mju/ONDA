package com.mju.onda.driver.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.feature.home.data.MockTodayOperations
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder
import com.mju.onda.driver.feature.settings.data.MockStopRequestDetail
import com.mju.onda.driver.feature.settings.data.StopRequestDraft
import com.mju.onda.driver.feature.settings.data.StopRequestDraftHolder
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StopRequestDetailUiState(
    val routeName: String = "",
    val vehicleName: String = "",
    val selectedReason: String = "",
    val message: String = "",
    val attachLocation: Boolean = true,
    val contactable: Boolean = true,
) {
    val canSubmit: Boolean
        get() = attachLocation &&
            contactable &&
            message.length >= MockStopRequestDetail.MIN_MESSAGE_LENGTH
}

sealed interface StopRequestDetailEvent {
    data object NavigateBack : StopRequestDetailEvent
    data object Submitted : StopRequestDetailEvent
}

class StopRequestDetailViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(StopRequestDetailUiState())
    val uiState: StateFlow<StopRequestDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<StopRequestDetailEvent>()
    val events: SharedFlow<StopRequestDetailEvent> = _events.asSharedFlow()

    fun load(selectedReason: String) {
        val operationId = OperationRuntimeStateHolder.activeOperationId()
        val operation = operationId?.let { MockTodayOperations.findById(it) }
            ?: MockTodayOperations.assignedOperations.firstOrNull()
            ?: return
        _uiState.update {
            it.copy(
                routeName = operation.routeName,
                vehicleName = operation.vehicleName,
                selectedReason = selectedReason,
                message = "",
            )
        }
    }

    fun onMessageChange(value: String) {
        _uiState.update {
            it.copy(message = value.take(MockStopRequestDetail.MAX_MESSAGE_LENGTH))
        }
    }

    fun onAttachLocationChange(enabled: Boolean) {
        _uiState.update { it.copy(attachLocation = enabled) }
    }

    fun onContactableChange(enabled: Boolean) {
        _uiState.update { it.copy(contactable = enabled) }
    }

    fun onSubmit() {
        val state = _uiState.value
        if (!state.canSubmit) return
        StopRequestDraftHolder.set(
            StopRequestDraft(
                reason = state.selectedReason,
                routeName = state.routeName,
                vehicleName = state.vehicleName,
                locationLabel = MockStopRequestDetail.LOCATION_FALLBACK,
                includeLocation = state.attachLocation,
                message = state.message,
                contactable = state.contactable,
            ),
        )
        viewModelScope.launch { _events.emit(StopRequestDetailEvent.Submitted) }
    }

    fun onPrevious() {
        viewModelScope.launch { _events.emit(StopRequestDetailEvent.NavigateBack) }
    }

    fun onBack() {
        viewModelScope.launch { _events.emit(StopRequestDetailEvent.NavigateBack) }
    }
}
