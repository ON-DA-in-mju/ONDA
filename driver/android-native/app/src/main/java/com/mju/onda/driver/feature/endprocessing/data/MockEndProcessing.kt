package com.mju.onda.driver.feature.endprocessing.data

import com.mju.onda.driver.feature.startprocessing.data.ProcessingStep
import com.mju.onda.driver.feature.startprocessing.data.ProcessingStepStatus

object MockEndProcessing {
    const val SCREEN_TITLE = "운행 종료"
    const val HEADLINE = "운행을 종료하고 있습니다"
    const val SUBTITLE = "잠시만 기다려 주세요."
    const val FOOTER_INFO = "앱을 종료하거나 화면을 꺼도\n종료 처리는 계속 진행됩니다."
    const val DONE_LABEL = "완료"
    const val PROCESSING_LABEL = "처리 중..."

    data class StepDef(
        val id: String,
        val title: String,
        val inProgressTitle: String = title,
        val inProgressSubtitle: String = PROCESSING_LABEL,
    )

    val stepDefs: List<StepDef> = listOf(
        StepDef(
            id = "location_stop",
            title = "위치 전송 중단",
            inProgressSubtitle = "중단 중...",
        ),
        StepDef(
            id = "record_save",
            title = "운행 기록 저장",
            inProgressSubtitle = "저장 중...",
        ),
        StepDef(
            id = "server_status",
            title = "서버 상태 변경",
            inProgressSubtitle = "변경 중...",
        ),
        StepDef(
            id = "sync",
            title = "운행 상태 동기화",
            inProgressTitle = "운행 상태 동기화 중",
            inProgressSubtitle = PROCESSING_LABEL,
        ),
    )

    val initialSteps: List<ProcessingStep> = stepDefs.mapIndexed { index, def ->
        when (index) {
            0 -> ProcessingStep(
                id = def.id,
                title = def.inProgressTitle,
                subtitle = def.inProgressSubtitle,
                status = ProcessingStepStatus.InProgress,
            )
            else -> ProcessingStep(
                id = def.id,
                title = def.title,
                status = ProcessingStepStatus.Pending,
            )
        }
    }
}
