package com.mju.onda.driver.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.feature.settings.data.ContactInfoRow
import com.mju.onda.driver.feature.settings.data.InquiryTypeItem
import com.mju.onda.driver.feature.settings.data.MockContactAdmin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ContactAdminUiState(
    val contactRows: List<ContactInfoRow> = MockContactAdmin.contactRows,
    val inquiryTypes: List<InquiryTypeItem> = MockContactAdmin.inquiryTypes,
)

sealed interface ContactAdminEvent {
    data object NavigateBack : ContactAdminEvent
    data object CallAdmin : ContactAdminEvent
    data object EmailInquiry : ContactAdminEvent
    data class InquiryTypeSelected(val label: String) : ContactAdminEvent
}

class ContactAdminViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ContactAdminUiState())
    val uiState: StateFlow<ContactAdminUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ContactAdminEvent>()
    val events: SharedFlow<ContactAdminEvent> = _events.asSharedFlow()

    fun onBack() {
        viewModelScope.launch { _events.emit(ContactAdminEvent.NavigateBack) }
    }

    fun onCallAdmin() {
        viewModelScope.launch { _events.emit(ContactAdminEvent.CallAdmin) }
    }

    fun onEmailInquiry() {
        viewModelScope.launch { _events.emit(ContactAdminEvent.EmailInquiry) }
    }

    fun onInquiryTypeClick(item: InquiryTypeItem) {
        viewModelScope.launch {
            _events.emit(ContactAdminEvent.InquiryTypeSelected(item.label))
        }
    }
}
