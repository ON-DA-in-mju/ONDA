package com.mju.onda.driver.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.feature.consent.data.LocationConsentPrefs
import com.mju.onda.driver.feature.settings.data.ConsentStatusRow
import com.mju.onda.driver.feature.settings.data.MockLocationConsentManage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LocationConsentManageDialog {
    None,
    PrivacyPolicy,
    ConsentGuide,
    AgreeConfirm,
    RevokeConfirm,
}

data class LocationConsentManageUiState(
    val rows: List<ConsentStatusRow> = emptyList(),
    val statusBadge: String = MockLocationConsentManage.STATUS_BADGE_DENIED,
    val isConsented: Boolean = false,
    val dialog: LocationConsentManageDialog = LocationConsentManageDialog.None,
)

sealed interface LocationConsentManageEvent {
    data object NavigateBack : LocationConsentManageEvent
    data class ShowToast(val message: String) : LocationConsentManageEvent
}

class LocationConsentManageViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LocationConsentManageUiState())
    val uiState: StateFlow<LocationConsentManageUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LocationConsentManageEvent>()
    val events: SharedFlow<LocationConsentManageEvent> = _events.asSharedFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.update {
            it.copy(
                rows = MockLocationConsentManage.rows(),
                statusBadge = LocationConsentPrefs.statusBadge(),
                isConsented = LocationConsentPrefs.isConsented,
            )
        }
    }

    fun onBack() {
        viewModelScope.launch { _events.emit(LocationConsentManageEvent.NavigateBack) }
    }

    fun onPrivacyPolicy() {
        _uiState.update { it.copy(dialog = LocationConsentManageDialog.PrivacyPolicy) }
    }

    fun onConsentGuide() {
        _uiState.update { it.copy(dialog = LocationConsentManageDialog.ConsentGuide) }
    }

    fun onAgreeClick() {
        if (_uiState.value.isConsented) return
        _uiState.update { it.copy(dialog = LocationConsentManageDialog.AgreeConfirm) }
    }

    fun onRevokeClick() {
        if (!_uiState.value.isConsented) return
        _uiState.update { it.copy(dialog = LocationConsentManageDialog.RevokeConfirm) }
    }

    fun confirmAgree() {
        LocationConsentPrefs.markConsented()
        _uiState.update { it.copy(dialog = LocationConsentManageDialog.None) }
        refresh()
        viewModelScope.launch {
            _events.emit(LocationConsentManageEvent.ShowToast(MockLocationConsentManage.TOAST_AGREED))
        }
    }

    fun confirmRevoke() {
        LocationConsentPrefs.revokeConsent()
        _uiState.update { it.copy(dialog = LocationConsentManageDialog.None) }
        refresh()
        viewModelScope.launch {
            _events.emit(LocationConsentManageEvent.ShowToast(MockLocationConsentManage.TOAST_REVOKED))
        }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(dialog = LocationConsentManageDialog.None) }
    }
}
