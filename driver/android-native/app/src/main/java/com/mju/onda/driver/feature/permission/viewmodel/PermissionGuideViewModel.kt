package com.mju.onda.driver.feature.permission.viewmodel

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.core.location.OperationDeviceStatus
import com.mju.onda.driver.feature.auth.data.SessionStateHolder
import com.mju.onda.driver.feature.permission.data.MockPermissionGuide
import com.mju.onda.driver.feature.permission.data.PermissionGuideItem
import com.mju.onda.driver.feature.permission.data.PermissionStateHolder
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PermissionGuideUiState(
    val items: List<PermissionGuideItem> = MockPermissionGuide.items,
    val isRequesting: Boolean = false,
    val showLaterDialog: Boolean = false,
)

sealed interface PermissionGuideEvent {
    data object NavigateToPermissionComplete : PermissionGuideEvent
    /** 나중에 설정 → 완료 화면 없이 홈 진입 */
    data object NavigateToTodayOperation : PermissionGuideEvent
    data object NavigateBack : PermissionGuideEvent
    /** 전경 위치(+알림) 권한 요청 */
    data object RequestSystemLocationPermissions : PermissionGuideEvent
    /** Android 10+ 백그라운드 위치 (전경 허용 후 별도 요청) */
    data object RequestBackgroundLocationPermission : PermissionGuideEvent
}

class PermissionGuideViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PermissionGuideUiState())
    val uiState: StateFlow<PermissionGuideUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PermissionGuideEvent>()
    val events: SharedFlow<PermissionGuideEvent> = _events.asSharedFlow()

    fun onSetupPermissions() {
        if (_uiState.value.isRequesting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRequesting = true, showLaterDialog = false) }
            _events.emit(PermissionGuideEvent.RequestSystemLocationPermissions)
        }
    }

    /**
     * 전경 위치 결과 후:
     * - 허용 + 백그라운드 미허용(Q+) → 백그라운드 권한 이어서 요청
     * - 그 외 → OS 동기화 후 완료 화면
     */
    fun onForegroundPermissionResult(context: Context, locationGranted: Boolean) {
        viewModelScope.launch {
            if (locationGranted &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                !OperationDeviceStatus.hasBackgroundLocationPermission(context)
            ) {
                _events.emit(PermissionGuideEvent.RequestBackgroundLocationPermission)
                return@launch
            }
            finishPermissionFlow(context, locationGranted)
        }
    }

    /** 백그라운드 위치 다이얼로그 결과(허용/거부 모두) → 완료 화면 */
    fun onBackgroundPermissionResult(context: Context) {
        viewModelScope.launch {
            finishPermissionFlow(context, locationGranted = true)
        }
    }

    private suspend fun finishPermissionFlow(context: Context, locationGranted: Boolean) {
        PermissionStateHolder.syncFromSystem(context)
        if (locationGranted) {
            PermissionStateHolder.clearSkippedForLater()
        } else {
            PermissionStateHolder.markSkippedForLater()
        }
        _uiState.update { it.copy(isRequesting = false) }
        _events.emit(PermissionGuideEvent.NavigateToPermissionComplete)
    }

    fun onSetupLaterClick() {
        if (_uiState.value.isRequesting) return
        _uiState.update { it.copy(showLaterDialog = true) }
    }

    fun dismissLaterDialog() {
        _uiState.update { it.copy(showLaterDialog = false) }
    }

    /** 다이얼로그에서 [나중에] 선택 → 홈까지 진입 가능 */
    fun confirmSkipForLater(context: Context) {
        if (_uiState.value.isRequesting) return

        viewModelScope.launch {
            PermissionStateHolder.syncFromSystem(context)
            PermissionStateHolder.markSkippedForLater()
            SessionStateHolder.markOnboardingDone()
            _uiState.update { it.copy(showLaterDialog = false) }
            _events.emit(PermissionGuideEvent.NavigateToTodayOperation)
        }
    }

    fun onBack() {
        viewModelScope.launch {
            _events.emit(PermissionGuideEvent.NavigateBack)
        }
    }
}
