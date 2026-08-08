package com.mju.onda.driver.feature.auth.viewmodel



import androidx.lifecycle.ViewModel

import androidx.lifecycle.viewModelScope

import com.mju.onda.driver.core.login.LoginHistoryReporter

import com.mju.onda.driver.data.mock.MockDriver

import com.mju.onda.driver.feature.auth.data.AuthResult

import com.mju.onda.driver.feature.auth.data.MockAuthRepository

import com.mju.onda.driver.feature.auth.data.SessionStateHolder

import kotlinx.coroutines.flow.MutableSharedFlow

import kotlinx.coroutines.flow.MutableStateFlow

import kotlinx.coroutines.flow.SharedFlow

import kotlinx.coroutines.flow.StateFlow

import kotlinx.coroutines.flow.asSharedFlow

import kotlinx.coroutines.flow.asStateFlow

import kotlinx.coroutines.flow.update

import kotlinx.coroutines.launch



data class LoginUiState(

    val id: String = "",

    val password: String = "",

    val autoLogin: Boolean = SessionStateHolder.autoLoginEnabled,

    val obscurePassword: Boolean = true,

    val isLoading: Boolean = false,

    val errorMessage: String? = null,

    val currentDriver: MockDriver? = null,

)



sealed interface LoginEvent {

    data object NavigateToLocationConsent : LoginEvent

    data object ShowHelpMessage : LoginEvent

}



class LoginViewModel(

    private val authRepository: MockAuthRepository = MockAuthRepository(),

) : ViewModel() {



    private val _uiState = MutableStateFlow(LoginUiState())

    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()



    private val _events = MutableSharedFlow<LoginEvent>()

    val events: SharedFlow<LoginEvent> = _events.asSharedFlow()



    fun onIdChange(value: String) {

        _uiState.update { it.copy(id = value, errorMessage = null) }

    }



    fun onPasswordChange(value: String) {

        _uiState.update { it.copy(password = value, errorMessage = null) }

    }



    fun onAutoLoginChange(checked: Boolean) {

        _uiState.update { it.copy(autoLogin = checked) }

    }



    fun togglePasswordVisibility() {

        _uiState.update { it.copy(obscurePassword = !it.obscurePassword) }

    }



    fun onHelpClick() {

        viewModelScope.launch {

            _events.emit(LoginEvent.ShowHelpMessage)

        }

    }



    fun login() {

        val current = _uiState.value

        if (current.isLoading) return



        viewModelScope.launch {

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }



            when (val result = authRepository.login(current.id, current.password)) {

                is AuthResult.Success -> {

                    SessionStateHolder.markLoggedIn(

                        userId = result.driver.id,

                        autoLogin = current.autoLogin,

                    )

                    LoginHistoryReporter.report(

                        userId = result.driver.id,

                        name = result.driver.name,

                    )

                    _uiState.update {

                        it.copy(

                            isLoading = false,

                            currentDriver = result.driver,

                            errorMessage = null,

                        )

                    }

                    _events.emit(LoginEvent.NavigateToLocationConsent)

                }



                is AuthResult.Failure -> {

                    _uiState.update {

                        it.copy(isLoading = false, errorMessage = result.message)

                    }

                }

            }

        }

    }

}

