package com.mju.onda.driver.feature.startprocessing.data

enum class ProcessingStepStatus {
    Completed,
    InProgress,
    Pending,
    Failed,
}

data class ProcessingStep(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val status: ProcessingStepStatus,
)

object MockStartProcessing {
    const val SCREEN_TITLE = "운행 시작 중"
    const val HEADLINE = "위치 전송을 시작하고 있습니다"
    const val SUBTITLE_LINE1 = "잠시만 기다려 주세요."
    const val SUBTITLE_LINE2 = "모든 준비가 완료되면 자동으로 운행이 시작됩니다."
    const val FOOTER_INFO = "앱을 종료하거나 화면을 끄더라도 위치 전송은 계속됩니다."
    const val DONE_LABEL = "완료"
    const val FAIL_LABEL = "실패"
    const val COMPLETE_PENDING_TOAST = "운행 시작 완료 화면은 다음 단계에서 연결합니다."
    const val STEP_FAILED_TOAST = "준비에 실패한 항목이 있습니다. 권한·GPS·네트워크를 확인해 주세요."

    data class StepDef(
        val id: String,
        val title: String,
        val inProgressTitle: String = title,
        val inProgressSubtitle: String,
    )

    val stepDefs: List<StepDef> = listOf(
        StepDef(
            id = "info",
            title = "운행 정보 확인",
            inProgressSubtitle = "확인 중...",
        ),
        StepDef(
            id = "permission",
            title = "권한 및 기기 점검",
            inProgressSubtitle = "점검 중...",
        ),
        StepDef(
            id = "gps",
            title = "GPS 연결",
            inProgressTitle = "GPS 연결 중",
            inProgressSubtitle = "연결 시도 중...",
        ),
        StepDef(
            id = "location",
            title = "위치 전송 시작",
            inProgressSubtitle = "전송 중...",
        ),
        StepDef(
            id = "status",
            title = "운행 상태 업데이트",
            inProgressSubtitle = "업데이트 중...",
        ),
    )

    /** 첫 단계(운행 정보 확인)부터 로딩 시작 */
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
