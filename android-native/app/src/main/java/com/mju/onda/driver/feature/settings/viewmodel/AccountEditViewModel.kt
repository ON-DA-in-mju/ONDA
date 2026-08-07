package com.mju.onda.driver.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.feature.settings.data.AccountInfo
import com.mju.onda.driver.feature.settings.data.AccountInfoStateHolder
import com.mju.onda.driver.feature.settings.data.MockAccountInfo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountEditUiState(
    /** 표시명에서 "기사님"을 제외한 이름만 */
    val givenName: String = "",
    val driverId: String = "",
    val organization: String = "",
    val vehicleName: String = "",
    val contactStatus: String = "",
) {
    companion object {
        fun from(info: AccountInfo) = AccountEditUiState(
            givenName = MockAccountInfo.extractGivenName(info.driverName),
            driverId = info.driverId,
            organization = info.organization,
            vehicleName = info.vehicleName,
            contactStatus = info.contactStatus,
        )
    }
}

sealed interface AccountEditEvent {
    data object NavigateBack : AccountEditEvent
    data object Saved : AccountEditEvent
}

class AccountEditViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        AccountEditUiState.from(AccountInfoStateHolder.get()),
    )
    val uiState: StateFlow<AccountEditUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AccountEditEvent>()
    val events: SharedFlow<AccountEditEvent> = _events.asSharedFlow()

    fun onGivenNameChange(value: String) {
        // "기사님" 입력 방지 및 접미사 제거
        val cleaned = value
            .replace(MockAccountInfo.NAME_SUFFIX.trim(), "")
            .replace("기사님", "")
            .trimStart()
        _uiState.update { it.copy(givenName = cleaned) }
    }

    fun onOrganizationChange(value: String) {
        _uiState.update { it.copy(organization = value) }
    }

    fun onVehicleNameChange(value: String) {
        _uiState.update { it.copy(vehicleName = value) }
    }

    fun onContactStatusChange(value: String) {
        _uiState.update { it.copy(contactStatus = value) }
    }

    fun onSave() {
        val state = _uiState.value
        val base = AccountInfoStateHolder.get()
        AccountInfoStateHolder.update(
            base.copy(
                driverName = MockAccountInfo.formatDisplayName(state.givenName),
                organization = state.organization.trim().ifBlank { base.organization },
                vehicleName = state.vehicleName.trim().ifBlank { base.vehicleName },
                contactStatus = state.contactStatus.trim().ifBlank { base.contactStatus },
            ),
        )
        viewModelScope.launch { _events.emit(AccountEditEvent.Saved) }
    }

    fun onCancel() {
        viewModelScope.launch { _events.emit(AccountEditEvent.NavigateBack) }
    }

    fun onBack() {
        viewModelScope.launch { _events.emit(AccountEditEvent.NavigateBack) }
    }
}
