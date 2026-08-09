package com.mju.onda.driver.feature.endconfirm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.feature.endconfirm.data.EndOperationConfirmInfo
import com.mju.onda.driver.feature.endconfirm.data.MockEndOperationConfirm
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class EndOperationConfirmUiState(
    val info: EndOperationConfirmInfo = MockEndOperationConfirm.forOperationId(
        OperationRuntimeStateHolder.resolveFocusedOperationId(),
    ),
    val showConfirmDialog: Boolean = false,
)

sealed interface EndOperationConfirmEvent {
    data object NavigateBack : EndOperationConfirmEvent
    /** 확인 → 운행 종료 처리(DRI-01-04B)로 이동 (아직 실제 종료는 아님) */
    data object GoToEndProcessing : EndOperationConfirmEvent
}

class EndOperationConfirmViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(EndOperationConfirmUiState())
    val uiState: StateFlow<EndOperationConfirmUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EndOperationConfirmEvent>()
    val events: SharedFlow<EndOperationConfirmEvent> = _events.asSharedFlow()

    private var operationId: String = OperationRuntimeStateHolder.resolveFocusedOperationId()
    private var tickJob: Job? = null

    fun load(operationId: String) {
        this.operationId = operationId
        refresh()
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                refresh()
            }
        }
    }

    private fun refresh() {
        _uiState.update {
            it.copy(info = MockEndOperationConfirm.forOperationId(operationId))
        }
    }

    fun onBack() {
        viewModelScope.launch { _events.emit(EndOperationConfirmEvent.NavigateBack) }
    }

    fun onConfirmEnd() {
        _uiState.update { it.copy(showConfirmDialog = true) }
    }

    fun dismissConfirmDialog() {
        _uiState.update { it.copy(showConfirmDialog = false) }
    }

    fun onDialogConfirmYes() {
        _uiState.update { it.copy(showConfirmDialog = false) }
        viewModelScope.launch { _events.emit(EndOperationConfirmEvent.GoToEndProcessing) }
    }
}
