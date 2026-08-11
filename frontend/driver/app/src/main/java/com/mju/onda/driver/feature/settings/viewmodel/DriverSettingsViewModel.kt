package com.mju.onda.driver.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder
import com.mju.onda.driver.feature.settings.data.AccountInfoStateHolder
import com.mju.onda.driver.feature.settings.data.DriverProfile
import com.mju.onda.driver.feature.settings.data.MockDriverSettings
import com.mju.onda.driver.feature.settings.data.SettingsMenuId
import com.mju.onda.driver.feature.settings.data.SettingsMenuItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DriverSettingsUiState(
    val profile: DriverProfile = AccountInfoStateHolder.toProfile(),
    val menuItems: List<SettingsMenuItem> = MockDriverSettings.menuItems,
)

sealed interface DriverSettingsEvent {
    data object NavigateBack : DriverSettingsEvent
    data object OpenLogoutConfirm : DriverSettingsEvent
    data object OpenLogoutRestricted : DriverSettingsEvent
    data object MenuPending : DriverSettingsEvent
    data object OpenAccountInfo : DriverSettingsEvent
    data object OpenDevicePermission : DriverSettingsEvent
    data object OpenAlarmSettings : DriverSettingsEvent
    data object OpenLocationConsentManage : DriverSettingsEvent
    data object OpenContactAdmin : DriverSettingsEvent
    data object OpenSafeStopHistory : DriverSettingsEvent
    data object NotInOperation : DriverSettingsEvent
}

class DriverSettingsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DriverSettingsUiState())
    val uiState: StateFlow<DriverSettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<DriverSettingsEvent>()
    val events: SharedFlow<DriverSettingsEvent> = _events.asSharedFlow()

    fun refresh() {
        _uiState.update { it.copy(profile = AccountInfoStateHolder.toProfile()) }
    }

    fun onBack() {
        viewModelScope.launch { _events.emit(DriverSettingsEvent.NavigateBack) }
    }

    fun onProfileClick() {
        viewModelScope.launch { _events.emit(DriverSettingsEvent.OpenAccountInfo) }
    }

    fun onMenuClick(id: SettingsMenuId) {
        viewModelScope.launch {
            when (id) {
                SettingsMenuId.DevicePermission ->
                    _events.emit(DriverSettingsEvent.OpenDevicePermission)
                SettingsMenuId.Alarm ->
                    _events.emit(DriverSettingsEvent.OpenAlarmSettings)
                SettingsMenuId.LocationConsent ->
                    _events.emit(DriverSettingsEvent.OpenLocationConsentManage)
                SettingsMenuId.ContactAdmin ->
                    _events.emit(DriverSettingsEvent.OpenContactAdmin)
                SettingsMenuId.SafeStop ->
                    _events.emit(DriverSettingsEvent.OpenSafeStopHistory)
                else -> _events.emit(DriverSettingsEvent.MenuPending)
            }
        }
    }

    fun onLogout() {
        viewModelScope.launch {
            // 홈 배차는 비었는데 로컬만 운행중으로 남은 유령 상태 정리
            OperationRuntimeStateHolder.clearOrphanedActiveOperations()
            if (OperationRuntimeStateHolder.hasActiveOperation()) {
                _events.emit(DriverSettingsEvent.OpenLogoutRestricted)
            } else {
                _events.emit(DriverSettingsEvent.OpenLogoutConfirm)
            }
        }
    }
}
