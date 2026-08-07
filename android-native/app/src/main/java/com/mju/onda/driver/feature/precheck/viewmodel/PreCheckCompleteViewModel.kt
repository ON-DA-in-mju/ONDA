package com.mju.onda.driver.feature.precheck.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.feature.precheck.data.CompletedCheckItem
import com.mju.onda.driver.feature.precheck.data.MockPreCheckComplete
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PreCheckCompleteUiState(
    val items: List<CompletedCheckItem> = MockPreCheckComplete.items,
)

sealed interface PreCheckCompleteEvent {
    data object NavigateBack : PreCheckCompleteEvent
    data object OpenStartConfirm : PreCheckCompleteEvent
}

class PreCheckCompleteViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PreCheckCompleteUiState())
    val uiState: StateFlow<PreCheckCompleteUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PreCheckCompleteEvent>()
    val events: SharedFlow<PreCheckCompleteEvent> = _events.asSharedFlow()

    fun onBack() {
        viewModelScope.launch { _events.emit(PreCheckCompleteEvent.NavigateBack) }
    }

    fun onStartOperation() {
        viewModelScope.launch { _events.emit(PreCheckCompleteEvent.OpenStartConfirm) }
    }
}
