package com.mju.onda.driver.feature.home.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.feature.alarm.data.AlarmGenerator
import com.mju.onda.driver.feature.alarm.data.MockOperationAlarms
import com.mju.onda.driver.feature.auth.data.SessionStateHolder
import com.mju.onda.driver.feature.home.data.AssignedOperation
import com.mju.onda.driver.feature.home.data.MockTodayOperations
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder
import com.mju.onda.driver.feature.home.data.OperationStatus
import com.mju.onda.driver.feature.home.data.TodayAssignmentsApi
import com.mju.onda.driver.feature.home.data.TodayAssignmentsHolder
import com.mju.onda.driver.feature.permission.data.PermissionStateHolder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class DepartureHomeAlert {
    None,
    Imminent, // DRI-01-02E
    Overdue, // DRI-01-02F
}

data class TodayOperationUiState(
    val operations: List<AssignedOperation> = emptyList(),
    val unreadAlarmCount: Int = MockOperationAlarms.unreadCount(),
    val isRefreshing: Boolean = false,
    val showPermissionRequiredDialog: Boolean = false,
    /** 배차 API 실패 시 안내 (빈 목록과 구분) */
    val loadError: String? = null,
    /** 기본 None — 하단 테스트 버튼으로만 표시 */
    val departureAlert: DepartureHomeAlert = DepartureHomeAlert.None,
) {
    val assignedCount: Int get() = operations.size
    val hasAssignments: Boolean get() = operations.isNotEmpty()
    /** 현재 운행 중인 배차 (있으면 홈 상단 블록에 표시) */
    val activeOperation: AssignedOperation?
        get() = operations.firstOrNull { it.status == OperationStatus.InProgress }
    /** 운행 중·종료 항목은 제외하고, 가장 가까운 예정 운행을 다음 운행으로 표시 */
    val nextOperation: AssignedOperation?
        get() = operations.firstOrNull {
            it.status != OperationStatus.InProgress &&
                it.status != OperationStatus.Ended &&
                it.status != OperationStatus.Unavailable
        }
}

/** 새로고침 이후(세션 저장)면 배정 목록 복원, 아니면 빈 목록 */
private fun restoredHomeOperations(): List<AssignedOperation> {
    if (!SessionStateHolder.assignmentsLoaded) return emptyList()
    return OperationRuntimeStateHolder.withRuntimeStatus(MockTodayOperations.assignedOperations)
}

sealed interface TodayOperationEvent {
    data class OpenDetail(val operationId: String) : TodayOperationEvent
    data class OpenInOperation(val operationId: String) : TodayOperationEvent
    data object ShowAlarmPending : TodayOperationEvent
    data object OpenHistory : TodayOperationEvent
    data object OpenSettings : TodayOperationEvent
    data object ContactAdmin : TodayOperationEvent
    data object OpenAppPermissionSettings : TodayOperationEvent
}

class TodayOperationViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        TodayOperationUiState(operations = restoredHomeOperations()),
    )
    val uiState: StateFlow<TodayOperationUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<TodayOperationEvent>()
    val events: SharedFlow<TodayOperationEvent> = _events.asSharedFlow()

    init {
        // 홈 진입 시 DB 오늘 배차 자동 조회
        onRefresh()
        viewModelScope.launch {
            while (isActive) {
                delay(30_000)
                syncRuntimeStatus()
            }
        }
    }

    fun onAlarmClick() {
        viewModelScope.launch {
            _events.emit(TodayOperationEvent.ShowAlarmPending)
        }
    }

    /**
     * 운행 중 → 운행 복구(DRI-01-03F), 그 외 → 배정 운행 상세.
     * 진입 전 필수 권한 검사 (OS 기준).
     */
    fun onDetailClick(context: Context, operationId: String) {
        if (!PermissionStateHolder.hasRequiredPermissions(context)) {
            _uiState.update { it.copy(showPermissionRequiredDialog = true) }
            return
        }

        val isInProgress = _uiState.value.operations
            .any { it.id == operationId && it.status == OperationStatus.InProgress }

        viewModelScope.launch {
            if (isInProgress) {
                _events.emit(TodayOperationEvent.OpenInOperation(operationId))
            } else {
                _events.emit(TodayOperationEvent.OpenDetail(operationId))
            }
        }
    }

    fun dismissPermissionRequiredDialog() {
        _uiState.update { it.copy(showPermissionRequiredDialog = false) }
    }

    fun onOpenPermissionSettings() {
        _uiState.update { it.copy(showPermissionRequiredDialog = false) }
        viewModelScope.launch {
            _events.emit(TodayOperationEvent.OpenAppPermissionSettings)
        }
    }

    fun onHistoryClick() {
        viewModelScope.launch {
            _events.emit(TodayOperationEvent.OpenHistory)
        }
    }

    fun onSettingsClick() {
        viewModelScope.launch {
            _events.emit(TodayOperationEvent.OpenSettings)
        }
    }

    fun onContactAdminClick() {
        viewModelScope.launch {
            _events.emit(TodayOperationEvent.ContactAdmin)
        }
    }

    /** 새로고침 → Supabase 오늘 배차 조회 (실패 시 mock 없음, 오류 메시지 표시) */
    fun onRefresh() {
        if (_uiState.value.isRefreshing) return

        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, loadError = null) }
            val previousOps = _uiState.value.operations
            val cachedBefore = TodayAssignmentsHolder.getOrNull().orEmpty()
            val userId = SessionStateHolder.currentUserId
            var loadError: String? = null
            when (
                val result = if (userId != null) {
                    TodayAssignmentsApi.fetchForDriver(userId)
                } else {
                    TodayAssignmentsApi.Result.Failed("로그인 정보 없음")
                }
            ) {
                is TodayAssignmentsApi.Result.Ok -> {
                    if (result.items.isEmpty() && cachedBefore.isNotEmpty()) {
                        // 서버가 일시적으로 0건이어도 기존 목록을 지우지 않음
                        loadError =
                            "서버에서 오늘 배차 0건이 내려왔습니다. 이전 목록을 유지합니다. (DB 시드/날짜를 확인하세요)"
                    } else if (result.items.isEmpty()) {
                        TodayAssignmentsHolder.set(emptyList())
                        loadError =
                            "오늘 배차가 DB에 없습니다. 관리자에게 배차 등록(시드)을 요청하세요."
                    } else {
                        TodayAssignmentsHolder.set(result.items)
                        OperationRuntimeStateHolder.reconcileWithFetchedAssignments(result.items)
                    }
                }
                is TodayAssignmentsApi.Result.Failed -> {
                    // 실패 시 기존 목록은 유지하고 안내만 표시
                    loadError = result.reason
                }
            }
            SessionStateHolder.markAssignmentsLoaded()
            val newOps = restoredHomeOperations()
            AlarmGenerator.checkDepartureOverdue(newOps)
            AlarmGenerator.checkNewAssignments(previousOps, newOps)
            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    operations = newOps,
                    loadError = loadError,
                    unreadAlarmCount = MockOperationAlarms.unreadCount(),
                )
            }
        }
    }

    /** 운행 시작 후 홈 복귀·시계 진행 시 배지(운행 예정 ↔ 곧 출발 ↔ 운행 중) 반영 */
    fun syncRuntimeStatus() {
        if (!SessionStateHolder.assignmentsLoaded) return
        val base = MockTodayOperations.assignedOperations
        val ops = OperationRuntimeStateHolder.withRuntimeStatus(base)
        AlarmGenerator.checkDepartureOverdue(ops)
        _uiState.update {
            it.copy(
                operations = ops,
                unreadAlarmCount = MockOperationAlarms.unreadCount(),
            )
        }
    }

    /** 서버/관리자 연동 시 호출: DRI-01-02E 출발시간 임박 배너 */
    fun onDemoShowImminentAlert() {
        _uiState.update { it.copy(departureAlert = DepartureHomeAlert.Imminent) }
    }

    /** 서버/관리자 연동 시 호출: DRI-01-02F 예정시간 경과_미시작 배너 */
    fun onDemoShowOverdueAlert() {
        _uiState.update { it.copy(departureAlert = DepartureHomeAlert.Overdue) }
    }

    fun onDemoHideDepartureAlert() {
        _uiState.update { it.copy(departureAlert = DepartureHomeAlert.None) }
    }
}
