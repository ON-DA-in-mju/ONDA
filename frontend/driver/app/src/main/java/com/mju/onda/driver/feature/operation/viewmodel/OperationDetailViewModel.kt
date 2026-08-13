package com.mju.onda.driver.feature.operation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.feature.home.data.MockTodayOperations
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder
import com.mju.onda.driver.feature.home.data.OperationStatus
import com.mju.onda.driver.feature.operation.data.MockOperationDetail
import com.mju.onda.driver.feature.operation.data.OperationDetailInfo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OperationDetailUiState(
    val info: OperationDetailInfo = MockOperationDetail.unknown(""),
    val showConfirmDialog: Boolean = false,
    /** 순차 시작 가능 배차만 운행 준비하기 활성 */
    val prepareEnabled: Boolean = false,
)

sealed interface OperationDetailEvent {
    data object NavigateBack : OperationDetailEvent
    data object PrepareConfirmed : OperationDetailEvent
    data object ContactAdmin : OperationDetailEvent
    data object OpenHistory : OperationDetailEvent
    data object OpenSettings : OperationDetailEvent
    data object GoHome : OperationDetailEvent
}

class OperationDetailViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(OperationDetailUiState())
    val uiState: StateFlow<OperationDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<OperationDetailEvent>()
    val events: SharedFlow<OperationDetailEvent> = _events.asSharedFlow()

    fun load(operationId: String) {
        val id = operationId.trim()
        if (id.isBlank()) {
            android.util.Log.w("OpDetail", "load skipped: blank operationId")
            _uiState.value = OperationDetailUiState(
                info = MockOperationDetail.unknown(id),
                prepareEnabled = false,
            )
            return
        }
        val info = resolveInfo(id)
        _uiState.value = OperationDetailUiState(
            info = info,
            prepareEnabled = info.status != OperationStatus.Ended &&
                info.status != OperationStatus.Unavailable &&
                info.status != OperationStatus.InProgress &&
                OperationRuntimeStateHolder.canStartOperation(id),
        )
    }

    fun onBack() {
        viewModelScope.launch { _events.emit(OperationDetailEvent.NavigateBack) }
    }

    /** 운행 준비하기 → DRI-01-02B 배정 정보 확인 */
    fun onPrepare() {
        if (!_uiState.value.prepareEnabled) return
        _uiState.update { it.copy(showConfirmDialog = true) }
    }

    fun dismissConfirmDialog() {
        _uiState.update { it.copy(showConfirmDialog = false) }
    }

    fun onConfirmInfoDifferent() {
        _uiState.update { it.copy(showConfirmDialog = false) }
        viewModelScope.launch { _events.emit(OperationDetailEvent.ContactAdmin) }
    }

    fun onConfirmInfoOk() {
        _uiState.update { it.copy(showConfirmDialog = false) }
        viewModelScope.launch { _events.emit(OperationDetailEvent.PrepareConfirmed) }
    }

    fun onContactAdmin() {
        viewModelScope.launch { _events.emit(OperationDetailEvent.ContactAdmin) }
    }

    fun onHistoryClick() {
        viewModelScope.launch { _events.emit(OperationDetailEvent.OpenHistory) }
    }

    fun onSettingsClick() {
        viewModelScope.launch { _events.emit(OperationDetailEvent.OpenSettings) }
    }

    fun onTodayClick() {
        viewModelScope.launch { _events.emit(OperationDetailEvent.GoHome) }
    }

    private companion object {
        /**
         * 바로 출발 가능 → 운행 대기(초록),
         * 그 외 예정 배차 → 운행 예정(파랑, 홈과 동일).
         */
        fun resolveInfo(operationId: String): OperationDetailInfo {
            val base = MockOperationDetail.forOperationId(operationId)
            val (status, label) = when {
                OperationRuntimeStateHolder.isEnded(operationId) ||
                    base.status == OperationStatus.Ended ->
                    OperationStatus.Ended to MockTodayOperations.ENDED_BADGE
                OperationRuntimeStateHolder.isInProgress(operationId) ->
                    OperationStatus.InProgress to MockTodayOperations.IN_PROGRESS_BADGE
                base.status == OperationStatus.Unavailable ->
                    OperationStatus.Unavailable to MockTodayOperations.UNAVAILABLE_BADGE
                OperationRuntimeStateHolder.canStartOperation(operationId) ->
                    OperationStatus.Waiting to MockTodayOperations.WAITING_BADGE
                else ->
                    OperationStatus.Scheduled to MockTodayOperations.SCHEDULED_BADGE
            }
            return base.copy(status = status, statusLabel = label)
        }
    }
}
