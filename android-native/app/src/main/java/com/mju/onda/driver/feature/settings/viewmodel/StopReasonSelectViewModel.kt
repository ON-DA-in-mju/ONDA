package com.mju.onda.driver.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.feature.settings.data.MockStopReasonSelect
import com.mju.onda.driver.feature.settings.data.StopReasonItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StopReasonSelectUiState(
    val reasons: List<StopReasonItem> = MockStopReasonSelect.reasons,
    val selectedId: String = MockStopReasonSelect.reasons.first().id,
    val otherDetail: String = "",
) {
    val selectedReason: StopReasonItem?
        get() = reasons.find { it.id == selectedId }

    val isOtherSelected: Boolean
        get() = selectedId == MockStopReasonSelect.OTHER_ID
}

sealed interface StopReasonSelectEvent {
    data object NavigateBack : StopReasonSelectEvent
    data class ProceedNext(val reasonLabel: String) : StopReasonSelectEvent
}

class StopReasonSelectViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(StopReasonSelectUiState())
    val uiState: StateFlow<StopReasonSelectUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<StopReasonSelectEvent>()
    val events: SharedFlow<StopReasonSelectEvent> = _events.asSharedFlow()

    fun onSelect(id: String) {
        _uiState.update { it.copy(selectedId = id) }
    }

    fun onOtherDetailChange(value: String) {
        // 길이 제한은 UI(TextFieldValue)에서 조합 종료 후 적용. 여기서는 안전장치만 둠.
        val clipped = if (value.length <= MockStopReasonSelect.OTHER_MAX_LENGTH) {
            value
        } else {
            value.take(MockStopReasonSelect.OTHER_MAX_LENGTH)
        }
        _uiState.update { it.copy(otherDetail = clipped) }
    }

    fun onNext() {
        val state = _uiState.value
        val reason = state.selectedReason ?: return
        val label = if (state.isOtherSelected && state.otherDetail.isNotBlank()) {
            "${reason.label} (${state.otherDetail.trim()})"
        } else {
            reason.label
        }
        viewModelScope.launch { _events.emit(StopReasonSelectEvent.ProceedNext(label)) }
    }

    fun onPrevious() {
        viewModelScope.launch { _events.emit(StopReasonSelectEvent.NavigateBack) }
    }

    fun onBack() {
        viewModelScope.launch { _events.emit(StopReasonSelectEvent.NavigateBack) }
    }
}
