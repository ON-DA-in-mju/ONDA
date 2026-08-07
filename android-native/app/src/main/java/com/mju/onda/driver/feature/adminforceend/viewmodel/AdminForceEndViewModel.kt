package com.mju.onda.driver.feature.adminforceend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.feature.adminforceend.data.AdminForceEndInfo
import com.mju.onda.driver.feature.adminforceend.data.MockAdminForceEnd
import com.mju.onda.driver.feature.history.data.HistoryRuntimeStateHolder
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminForceEndUiState(
    val info: AdminForceEndInfo = MockAdminForceEnd.forOperationId(
        OperationRuntimeStateHolder.resolveFocusedOperationId(),
    ),
)

sealed interface AdminForceEndEvent {
    data object NavigateBack : AdminForceEndEvent
    data object GoToTodayOperation : AdminForceEndEvent
    data object ContactAdmin : AdminForceEndEvent
}

class AdminForceEndViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AdminForceEndUiState())
    val uiState: StateFlow<AdminForceEndUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AdminForceEndEvent>()
    val events: SharedFlow<AdminForceEndEvent> = _events.asSharedFlow()

    private var operationId: String = "op-0905"

    fun load(operationId: String) {
        this.operationId = operationId
        OperationRuntimeStateHolder.endOperation(operationId)
        HistoryRuntimeStateHolder.recordAdminEnd(operationId)
        _uiState.value = AdminForceEndUiState(
            info = MockAdminForceEnd.forOperationId(operationId),
        )
    }

    fun onBack() {
        viewModelScope.launch { _events.emit(AdminForceEndEvent.NavigateBack) }
    }

    fun onGoToToday() {
        viewModelScope.launch { _events.emit(AdminForceEndEvent.GoToTodayOperation) }
    }

    fun onContactAdmin() {
        viewModelScope.launch { _events.emit(AdminForceEndEvent.ContactAdmin) }
    }
}
