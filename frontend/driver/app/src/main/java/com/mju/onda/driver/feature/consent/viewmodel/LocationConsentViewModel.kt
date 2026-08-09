package com.mju.onda.driver.feature.consent.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.feature.consent.data.LocationConsentItem
import com.mju.onda.driver.feature.consent.data.LocationConsentPrefs
import com.mju.onda.driver.feature.consent.data.MockLocationConsent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LocationConsentUiState(
    val items: List<LocationConsentItem> = MockLocationConsent.items,
    val agreed: Boolean = false,
    val showDetailDialog: Boolean = false,
    val showDisagreeDialog: Boolean = false,
)

sealed interface LocationConsentEvent {
    data object NavigateToPermissionGuide : LocationConsentEvent
    data object NavigateBack : LocationConsentEvent
}

class LocationConsentViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LocationConsentUiState())
    val uiState: StateFlow<LocationConsentUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LocationConsentEvent>()
    val events: SharedFlow<LocationConsentEvent> = _events.asSharedFlow()

    fun onAgree() {
        LocationConsentPrefs.markConsented()
        _uiState.update { it.copy(agreed = true) }
        viewModelScope.launch {
            _events.emit(LocationConsentEvent.NavigateToPermissionGuide)
        }
    }

    fun onDisagreeClick() {
        _uiState.update { it.copy(showDisagreeDialog = true) }
    }

    fun dismissDisagreeDialog() {
        _uiState.update { it.copy(showDisagreeDialog = false) }
    }

    fun confirmDisagree() {
        _uiState.update { it.copy(showDisagreeDialog = false, agreed = false) }
        viewModelScope.launch {
            _events.emit(LocationConsentEvent.NavigateBack)
        }
    }

    fun onDetailClick() {
        _uiState.update { it.copy(showDetailDialog = true) }
    }

    fun dismissDetailDialog() {
        _uiState.update { it.copy(showDetailDialog = false) }
    }

    fun onBack() {
        viewModelScope.launch {
            _events.emit(LocationConsentEvent.NavigateBack)
        }
    }
}
