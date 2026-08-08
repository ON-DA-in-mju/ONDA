package com.mju.onda.driver.feature.permission.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.feature.auth.data.SessionStateHolder
import com.mju.onda.driver.feature.permission.data.MockPermissionComplete
import com.mju.onda.driver.feature.permission.data.PermissionStateHolder
import com.mju.onda.driver.feature.permission.data.PermissionStatusItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PermissionCompleteUiState(
    val statusItems: List<PermissionStatusItem> = emptyList(),
)

sealed interface PermissionCompleteEvent {
    data object NavigateToTodayOperation : PermissionCompleteEvent
}

class PermissionCompleteViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PermissionCompleteUiState())
    val uiState: StateFlow<PermissionCompleteUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PermissionCompleteEvent>()
    val events: SharedFlow<PermissionCompleteEvent> = _events.asSharedFlow()

    /** OS 권한을 읽어 상태 목록을 갱신한다. */
    fun refreshFromSystem(context: Context) {
        PermissionStateHolder.syncFromSystem(context)
        _uiState.update {
            it.copy(
                statusItems = MockPermissionComplete.statusItems(
                    whenInUseLocationGranted = PermissionStateHolder.whenInUseLocationGranted,
                    preciseLocationGranted = PermissionStateHolder.preciseLocationGranted,
                    backgroundLocationGranted = PermissionStateHolder.backgroundLocationGranted,
                    notificationGranted = PermissionStateHolder.notificationGranted,
                ),
            )
        }
    }

    fun goToOperation() {
        viewModelScope.launch {
            SessionStateHolder.markOnboardingDone()
            _events.emit(PermissionCompleteEvent.NavigateToTodayOperation)
        }
    }
}
