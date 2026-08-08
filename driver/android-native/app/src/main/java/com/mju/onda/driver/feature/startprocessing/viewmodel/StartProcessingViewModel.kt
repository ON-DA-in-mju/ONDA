package com.mju.onda.driver.feature.startprocessing.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.core.location.LatestLocationHolder
import com.mju.onda.driver.core.location.OperationDeviceStatus
import com.mju.onda.driver.core.location.OperationLocationTracker
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder
import com.mju.onda.driver.feature.startprocessing.data.MockStartProcessing
import com.mju.onda.driver.feature.startprocessing.data.ProcessingStep
import com.mju.onda.driver.feature.startprocessing.data.ProcessingStepStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StartProcessingUiState(
    val steps: List<ProcessingStep> = MockStartProcessing.initialSteps,
    val failed: Boolean = false,
)

sealed interface StartProcessingEvent {
    data object NavigateBack : StartProcessingEvent
    data object ProcessingFinished : StartProcessingEvent
    data object ProcessingFailed : StartProcessingEvent
}

class StartProcessingViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(StartProcessingUiState())
    val uiState: StateFlow<StartProcessingUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<StartProcessingEvent>()
    val events: SharedFlow<StartProcessingEvent> = _events.asSharedFlow()

    init {
        runRealProgress()
    }

    fun onBack() {
        viewModelScope.launch { _events.emit(StartProcessingEvent.NavigateBack) }
    }

    private fun runRealProgress() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val defs = MockStartProcessing.stepDefs
            val operationId = OperationRuntimeStateHolder.peekPendingStartId()
                ?: OperationRuntimeStateHolder.resolveFocusedOperationId()

            for (index in defs.indices) {
                val current = defs[index]
                val next = defs.getOrNull(index + 1)
                markInProgress(current, next)

                val ok = when (current.id) {
                    "info" -> {
                        delay(600)
                        operationId.isNotBlank()
                    }
                    "permission" -> {
                        delay(700)
                        OperationDeviceStatus.hasLocationPermission(context) &&
                            OperationDeviceStatus.hasBackgroundLocationPermission(context)
                    }
                    "gps" -> {
                        delay(700)
                        OperationDeviceStatus.isGpsEnabled(context)
                    }
                    "location" -> {
                        OperationRuntimeStateHolder.startOperation(operationId)
                        waitForTrackingOrFix(operationId)
                    }
                    "status" -> {
                        delay(700)
                        OperationDeviceStatus.isNetworkConnected(context) &&
                            OperationLocationTracker.isTracking
                    }
                    else -> {
                        delay(700)
                        true
                    }
                }

                if (!ok) {
                    markFailed(current.id, failMessage(current.id))
                    _uiState.update { it.copy(failed = true) }
                    _events.emit(StartProcessingEvent.ProcessingFailed)
                    return@launch
                }

                markCompleted(current.id, current.title)
            }

            OperationRuntimeStateHolder.takePendingStartId()
            delay(1_200)
            _events.emit(StartProcessingEvent.ProcessingFinished)
        }
    }

    private suspend fun waitForTrackingOrFix(operationId: String): Boolean {
        repeat(16) {
            if (OperationLocationTracker.isTracking) {
                val fix = LatestLocationHolder.latest
                if (fix != null && fix.operationId == operationId) return true
            }
            delay(500)
        }
        // 실내 등에서 첫 좌표가 늦을 수 있어, 트래킹만 켜져도 통과
        return OperationLocationTracker.isTracking &&
            OperationLocationTracker.activeOperationId == operationId
    }

    private fun failMessage(stepId: String): String = when (stepId) {
        "info" -> "운행 정보 없음"
        "permission" -> "위치 권한 필요"
        "gps" -> "GPS 꺼짐"
        "location" -> "위치 전송 실패"
        "status" -> "네트워크/상태 확인 실패"
        else -> MockStartProcessing.FAIL_LABEL
    }

    private fun markInProgress(
        current: MockStartProcessing.StepDef,
        next: MockStartProcessing.StepDef?,
    ) {
        _uiState.update { state ->
            state.copy(
                steps = state.steps.map { step ->
                    when (step.id) {
                        current.id -> step.copy(
                            title = current.inProgressTitle,
                            subtitle = current.inProgressSubtitle,
                            status = ProcessingStepStatus.InProgress,
                        )
                        else -> step
                    }
                },
            )
        }
        // next remains pending until current completes
        next // keep for readability / future use
    }

    private fun markCompleted(stepId: String, title: String) {
        val defs = MockStartProcessing.stepDefs
        val index = defs.indexOfFirst { it.id == stepId }
        val next = defs.getOrNull(index + 1)
        _uiState.update { state ->
            state.copy(
                steps = state.steps.map { step ->
                    when (step.id) {
                        stepId -> step.copy(
                            title = title,
                            subtitle = MockStartProcessing.DONE_LABEL,
                            status = ProcessingStepStatus.Completed,
                        )
                        next?.id -> step.copy(
                            title = next.inProgressTitle,
                            subtitle = next.inProgressSubtitle,
                            status = ProcessingStepStatus.InProgress,
                        )
                        else -> step
                    }
                },
            )
        }
    }

    private fun markFailed(stepId: String, message: String) {
        _uiState.update { state ->
            state.copy(
                steps = state.steps.map { step ->
                    if (step.id == stepId) {
                        step.copy(
                            subtitle = message,
                            status = ProcessingStepStatus.Failed,
                        )
                    } else {
                        step
                    }
                },
            )
        }
    }
}
