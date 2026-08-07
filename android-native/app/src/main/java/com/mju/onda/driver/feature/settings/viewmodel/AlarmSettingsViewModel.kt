package com.mju.onda.driver.feature.settings.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.feature.settings.data.AlarmSettingItem
import com.mju.onda.driver.feature.settings.data.AlarmSettingsStateHolder
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AlarmSettingsUiState(
    val notificationsEnabled: Boolean = false,
    val items: List<AlarmSettingItem> = AlarmSettingsStateHolder.get(),
    val savedItems: List<AlarmSettingItem> = AlarmSettingsStateHolder.get(),
) {
    val hasChanges: Boolean
        get() = items.map { it.id to it.enabled } != savedItems.map { it.id to it.enabled }
}

sealed interface AlarmSettingsEvent {
    data object NavigateBack : AlarmSettingsEvent
    data object GoToSettings : AlarmSettingsEvent
    data object Saved : AlarmSettingsEvent
    /** 시스템 알림 권한 요청 (Android 13+) */
    data object RequestNotificationPermission : AlarmSettingsEvent
    /** 앱 알림 설정 화면으로 이동 */
    data object OpenAppNotificationSettings : AlarmSettingsEvent
}

class AlarmSettingsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AlarmSettingsUiState())
    val uiState: StateFlow<AlarmSettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AlarmSettingsEvent>()
    val events: SharedFlow<AlarmSettingsEvent> = _events.asSharedFlow()

    fun refreshFromSystem(context: Context) {
        val enabled = areNotificationsEnabled(context)
        _uiState.update {
            it.copy(
                notificationsEnabled = enabled,
                items = AlarmSettingsStateHolder.get(),
                savedItems = AlarmSettingsStateHolder.get(),
            )
        }
    }

    fun onMasterToggle(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                // 켜기: 권한 요청 또는 설정 화면
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    _events.emit(AlarmSettingsEvent.RequestNotificationPermission)
                } else {
                    _events.emit(AlarmSettingsEvent.OpenAppNotificationSettings)
                }
            } else {
                // 끄기: 앱에서 직접 철회 불가 → 시스템 설정으로
                _events.emit(AlarmSettingsEvent.OpenAppNotificationSettings)
            }
        }
    }

    fun onNotificationPermissionResult(granted: Boolean, context: Context) {
        if (granted) {
            refreshFromSystem(context)
        } else {
            // 거부 시에도 설정 화면으로 안내 가능하도록 상태만 갱신
            refreshFromSystem(context)
        }
    }

    fun onToggle(id: String, enabled: Boolean) {
        if (!_uiState.value.notificationsEnabled) return
        _uiState.update { state ->
            state.copy(
                items = state.items.map { item ->
                    if (item.id == id) item.copy(enabled = enabled) else item
                },
            )
        }
    }

    fun onSave() {
        if (!_uiState.value.hasChanges) return
        if (!_uiState.value.notificationsEnabled) return
        persistAndNotifySaved()
    }

    fun onGoToSettings() {
        viewModelScope.launch { _events.emit(AlarmSettingsEvent.GoToSettings) }
    }

    fun onBack() {
        viewModelScope.launch { _events.emit(AlarmSettingsEvent.NavigateBack) }
    }

    private fun persistAndNotifySaved() {
        val items = _uiState.value.items
        AlarmSettingsStateHolder.update(items)
        _uiState.update { it.copy(savedItems = items) }
        viewModelScope.launch { _events.emit(AlarmSettingsEvent.Saved) }
    }

    companion object {
        fun areNotificationsEnabled(context: Context): Boolean {
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                return false
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                return ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            }
            return true
        }
    }
}
