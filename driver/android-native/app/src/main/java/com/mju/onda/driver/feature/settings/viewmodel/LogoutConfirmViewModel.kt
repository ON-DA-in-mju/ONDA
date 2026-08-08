package com.mju.onda.driver.feature.settings.viewmodel



import androidx.lifecycle.ViewModel

import androidx.lifecycle.viewModelScope

import com.mju.onda.driver.feature.auth.data.SessionStateHolder

import com.mju.onda.driver.feature.settings.data.AccountInfoStateHolder

import com.mju.onda.driver.feature.settings.data.MockLogoutConfirm

import kotlinx.coroutines.flow.MutableSharedFlow

import kotlinx.coroutines.flow.MutableStateFlow

import kotlinx.coroutines.flow.SharedFlow

import kotlinx.coroutines.flow.StateFlow

import kotlinx.coroutines.flow.asSharedFlow

import kotlinx.coroutines.flow.asStateFlow

import kotlinx.coroutines.flow.update

import kotlinx.coroutines.launch



data class LogoutConfirmUiState(

    val driverName: String = AccountInfoStateHolder.get().driverName,

    val subtitle: String = MockLogoutConfirm.subtitle(SessionStateHolder.autoLoginEnabled),

    val autoLoginLabel: String = MockLogoutConfirm.autoLoginLabel(SessionStateHolder.autoLoginEnabled),

)



sealed interface LogoutConfirmEvent {

    data object NavigateBack : LogoutConfirmEvent

    data object LoggedOut : LogoutConfirmEvent

}



class LogoutConfirmViewModel : ViewModel() {



    private val _uiState = MutableStateFlow(LogoutConfirmUiState())

    val uiState: StateFlow<LogoutConfirmUiState> = _uiState.asStateFlow()



    private val _events = MutableSharedFlow<LogoutConfirmEvent>()

    val events: SharedFlow<LogoutConfirmEvent> = _events.asSharedFlow()



    fun load() {

        val autoLogin = SessionStateHolder.autoLoginEnabled

        _uiState.update {

            it.copy(

                driverName = AccountInfoStateHolder.get().driverName,

                subtitle = MockLogoutConfirm.subtitle(autoLogin),

                autoLoginLabel = MockLogoutConfirm.autoLoginLabel(autoLogin),

            )

        }

    }



    fun onConfirmLogout() {

        SessionStateHolder.clear()

        viewModelScope.launch { _events.emit(LogoutConfirmEvent.LoggedOut) }

    }



    fun onCancel() {

        viewModelScope.launch { _events.emit(LogoutConfirmEvent.NavigateBack) }

    }



    fun onBack() {

        viewModelScope.launch { _events.emit(LogoutConfirmEvent.NavigateBack) }

    }

}

