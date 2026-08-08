package com.mju.onda.driver.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

sealed interface SafeStopConfirmEvent {
    data object NavigateBack : SafeStopConfirmEvent
    data class ProceedStopOperation(val operationId: String) : SafeStopConfirmEvent
    data class ReturnToOperation(val operationId: String?) : SafeStopConfirmEvent
}

class SafeStopConfirmViewModel : ViewModel() {

    private val _events = MutableSharedFlow<SafeStopConfirmEvent>()
    val events: SharedFlow<SafeStopConfirmEvent> = _events.asSharedFlow()

    fun onBack() {
        viewModelScope.launch { _events.emit(SafeStopConfirmEvent.NavigateBack) }
    }

    fun onConfirmSafeStop() {
        val operationId = OperationRuntimeStateHolder.activeOperationId() ?: return
        viewModelScope.launch {
            _events.emit(SafeStopConfirmEvent.ProceedStopOperation(operationId))
        }
    }

    fun onReturnToOperation() {
        viewModelScope.launch {
            _events.emit(
                SafeStopConfirmEvent.ReturnToOperation(
                    OperationRuntimeStateHolder.activeOperationId(),
                ),
            )
        }
    }
}
