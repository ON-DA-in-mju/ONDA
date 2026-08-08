package com.mju.onda.driver.feature.settings.data

data class InterruptedEndSummary(
    val routeName: String,
    val vehicleName: String,
    val reason: String,
    val actualStart: String,
    val interruptedAt: String,
    val totalDuration: String,
)

object MockInterruptedEndComplete {
    const val SCREEN_TITLE = "중단 운행 종료"
    const val BADGE = "운행 중단"
    const val HEADLINE = "중단된 운행이 종료되었습니다."
    const val SUBTITLE = "해당 운행은 중단 상태로 종료 처리되었습니다."

    const val LABEL_ROUTE = "노선"
    const val LABEL_VEHICLE = "차량"
    const val LABEL_REASON = "중단 사유"
    const val LABEL_ACTUAL_START = "실제 시작 시간"
    const val LABEL_INTERRUPTED_AT = "중단 처리 시간"
    const val LABEL_TOTAL = "총 운행 시간"

    const val GO_TODAY_LABEL = "오늘의 운행으로"
    const val HISTORY_LABEL = "운행 이력 보기"

    const val PROCESSING_TITLE = "운행 종료 처리"
    const val PROCESSING_HEADLINE = "운행을 종료하고 있습니다"
}
