package com.mju.onda.driver.feature.settings.viewmodel



import android.content.Context

import androidx.lifecycle.ViewModel

import androidx.lifecycle.viewModelScope

import com.mju.onda.driver.feature.settings.data.DeviceStatusItem

import com.mju.onda.driver.feature.settings.data.MockDevicePermission

import kotlinx.coroutines.delay

import kotlinx.coroutines.flow.MutableSharedFlow

import kotlinx.coroutines.flow.MutableStateFlow

import kotlinx.coroutines.flow.SharedFlow

import kotlinx.coroutines.flow.StateFlow

import kotlinx.coroutines.flow.asSharedFlow

import kotlinx.coroutines.flow.asStateFlow

import kotlinx.coroutines.flow.update

import kotlinx.coroutines.launch



data class DevicePermissionUiState(

    val items: List<DeviceStatusItem> = emptyList(),

    val isRefreshing: Boolean = false,

)



sealed interface DevicePermissionEvent {

    data object NavigateBack : DevicePermissionEvent

    data object OpenSystemSettings : DevicePermissionEvent

    data object RefreshDone : DevicePermissionEvent

}



class DevicePermissionViewModel : ViewModel() {



    private val _uiState = MutableStateFlow(DevicePermissionUiState())

    val uiState: StateFlow<DevicePermissionUiState> = _uiState.asStateFlow()



    private val _events = MutableSharedFlow<DevicePermissionEvent>()

    val events: SharedFlow<DevicePermissionEvent> = _events.asSharedFlow()



    fun refreshFromSystem(context: Context) {

        val notificationsEnabled = AlarmSettingsViewModel.areNotificationsEnabled(context)

        _uiState.update {

            it.copy(items = MockDevicePermission.items(context, notificationsEnabled))

        }

    }



    fun onBack() {

        viewModelScope.launch { _events.emit(DevicePermissionEvent.NavigateBack) }

    }



    fun onRefresh(context: Context) {

        if (_uiState.value.isRefreshing) return

        viewModelScope.launch {

            _uiState.update { it.copy(isRefreshing = true) }

            delay(400)

            refreshFromSystem(context)

            _uiState.update { it.copy(isRefreshing = false) }

            _events.emit(DevicePermissionEvent.RefreshDone)

        }

    }



    fun onOpenSettings() {

        viewModelScope.launch { _events.emit(DevicePermissionEvent.OpenSystemSettings) }

    }

}

