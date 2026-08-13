package com.mju.onda.driver.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.feature.settings.data.AccountInfo
import com.mju.onda.driver.feature.settings.data.AccountInfoStateHolder
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountInfoUiState(
    val info: AccountInfo = AccountInfoStateHolder.get(),
)

sealed interface AccountInfoEvent {
    data object NavigateBack : AccountInfoEvent
    data object GoToSettings : AccountInfoEvent
}

class AccountInfoViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AccountInfoUiState())
    val uiState: StateFlow<AccountInfoUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AccountInfoEvent>()
    val events: SharedFlow<AccountInfoEvent> = _events.asSharedFlow()

    fun refresh() {
        _uiState.update { it.copy(info = AccountInfoStateHolder.get()) }
    }

    fun onBack() {
        viewModelScope.launch { _events.emit(AccountInfoEvent.NavigateBack) }
    }

    fun onGoToSettings() {
        viewModelScope.launch { _events.emit(AccountInfoEvent.GoToSettings) }
    }
}
