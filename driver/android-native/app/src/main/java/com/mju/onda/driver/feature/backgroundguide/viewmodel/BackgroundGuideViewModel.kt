package com.mju.onda.driver.feature.backgroundguide.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.feature.backgroundguide.data.MockBackgroundGuide
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

sealed interface BackgroundGuideEvent {
    data object NavigateBack : BackgroundGuideEvent
}

class BackgroundGuideViewModel : ViewModel() {
    val items = MockBackgroundGuide.items

    private val _events = MutableSharedFlow<BackgroundGuideEvent>()
    val events: SharedFlow<BackgroundGuideEvent> = _events.asSharedFlow()

    fun onBack() {
        viewModelScope.launch { _events.emit(BackgroundGuideEvent.NavigateBack) }
    }

    fun onConfirm() {
        viewModelScope.launch { _events.emit(BackgroundGuideEvent.NavigateBack) }
    }
}
