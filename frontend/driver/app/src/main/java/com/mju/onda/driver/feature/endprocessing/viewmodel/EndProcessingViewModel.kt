package com.mju.onda.driver.feature.endprocessing.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.core.location.OperationLocationTracker
import com.mju.onda.driver.feature.endprocessing.data.MockEndProcessing
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder
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

data class EndProcessingUiState(
    val steps: List<ProcessingStep> = MockEndProcessing.initialSteps,
    val allDone: Boolean = false,
)

sealed interface EndProcessingEvent {
    data object ProcessingFinished : EndProcessingEvent
}

class EndProcessingViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(EndProcessingUiState())
    val uiState: StateFlow<EndProcessingUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EndProcessingEvent>()
    val events: SharedFlow<EndProcessingEvent> = _events.asSharedFlow()

    private var operationId: String = ""

    init {
        // operationId는 Screen에서 bind 후 시작
    }

    fun start(operationId: String) {
        if (this.operationId.isNotEmpty()) return
        this.operationId = operationId
        runRealProgress()
    }

    /** 위치 전송 중단을 실제로 수행한 뒤 나머지 단계를 진행 */
    private fun runRealProgress() {
        viewModelScope.launch {
            val defs = MockEndProcessing.stepDefs
            for (index in defs.indices) {
                val current = defs[index]
                val next = defs.getOrNull(index + 1)
                markInProgress(current)

                when (current.id) {
                    "location_stop" -> {
                        if (operationId.isNotBlank()) {
                            OperationRuntimeStateHolder.endOperation(operationId)
                        } else {
                            OperationLocationTracker.stop()
                        }
                        delay(800)
                    }
                    else -> delay(900)
                }

                markCompleted(current.id, current.title, next)
            }
            _uiState.update { it.copy(allDone = true) }
            delay(1_500)
            _events.emit(EndProcessingEvent.ProcessingFinished)
        }
    }

    private fun markInProgress(current: MockEndProcessing.StepDef) {
        _uiState.update { state ->
            state.copy(
                steps = state.steps.map { step ->
                    if (step.id == current.id) {
                        step.copy(
                            title = current.inProgressTitle,
                            subtitle = current.inProgressSubtitle,
                            status = ProcessingStepStatus.InProgress,
                        )
                    } else {
                        step
                    }
                },
            )
        }
    }

    private fun markCompleted(
        stepId: String,
        title: String,
        next: MockEndProcessing.StepDef?,
    ) {
        _uiState.update { state ->
            state.copy(
                steps = state.steps.map { step ->
                    when (step.id) {
                        stepId -> step.copy(
                            title = title,
                            subtitle = MockEndProcessing.DONE_LABEL,
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
}
