package com.mju.onda.driver.feature.startconfirm.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder
import com.mju.onda.driver.feature.operation.data.OperationDetailInfo
import com.mju.onda.driver.feature.startconfirm.data.MockStartConfirm
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
data class StartConfirmUiState(
    val info: OperationDetailInfo = MockStartConfirm.forOperationId(
        OperationRuntimeStateHolder.resolveFocusedOperationId(),
    ),
    val durationLabel: String = "",
    val roundLabel: String = "",
    val statusLabel: String = MockStartConfirm.statusLabel(),
    val showConfirmDialog: Boolean = false,
)
sealed interface StartConfirmEvent {
    data object NavigateBack : StartConfirmEvent
    data object ConfirmStart : StartConfirmEvent
}
class StartConfirmViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(buildState())
    val uiState: StateFlow<StartConfirmUiState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<StartConfirmEvent>()
    val events: SharedFlow<StartConfirmEvent> = _events.asSharedFlow()
    fun refresh() {
        _uiState.value = buildState()
    }
    fun onBack() {
        viewModelScope.launch { _events.emit(StartConfirmEvent.NavigateBack) }
    }
    fun onConfirmStart() {
        _uiState.update { it.copy(showConfirmDialog = true) }
    }
    fun dismissConfirmDialog() {
        _uiState.update { it.copy(showConfirmDialog = false) }
    }
    fun onDialogConfirmYes() {
        _uiState.update { it.copy(showConfirmDialog = false) }
        viewModelScope.launch { _events.emit(StartConfirmEvent.ConfirmStart) }
    }
    private companion object {
        fun buildState(): StartConfirmUiState {
            val info = MockStartConfirm.forOperationId(
                OperationRuntimeStateHolder.resolveFocusedOperationId(),
            )
            return StartConfirmUiState(
                info = info,
                durationLabel = MockStartConfirm.durationLabel(info),
                roundLabel = MockStartConfirm.roundLabel(info.round),
                statusLabel = MockStartConfirm.statusLabel(),
            )
        }
    }
}
