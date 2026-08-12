package com.mju.onda.driver.feature.endcomplete.data

import com.mju.onda.driver.core.OperationTripClock
import com.mju.onda.driver.feature.home.data.MockTodayOperations
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder

data class EndCompleteSummary(
    val routeName: String,
    val vehicleName: String,
    val scheduledDepart: String,
    val actualStart: String,
    val actualEnd: String,
    val totalDuration: String,
)

object MockEndComplete {
    const val SCREEN_TITLE = "운행 종료"
    const val HEADLINE = "운행을 종료하였습니다!"
    const val SUBTITLE = "잠시만 기다려 주세요."
    const val GO_TODAY_LABEL = "오늘의 운행으로"
    const val HISTORY_LABEL = "운행 이력 확인"
    const val HISTORY_TOAST = "운행 이력 화면은 다음 단계에서 연결합니다."
    const val FOOTER_INFO = "앱을 종료하거나 화면을 꺼도\n종료 처리는 계속 진행됩니다."

    const val LABEL_ROUTE = "노선"
    const val LABEL_VEHICLE = "차량"
    const val LABEL_SCHEDULED = "예정 출발 시간"
    const val LABEL_ACTUAL_START = "실제 시작 시간"
    const val LABEL_ACTUAL_END = "실제 종료 시간"
    const val LABEL_TOTAL = "총 운행 시간"

    fun forOperationId(operationId: String): EndCompleteSummary {
        val op = MockTodayOperations.assignedOperations.find { it.id == operationId }
            ?: MockTodayOperations.assignedOperations.first()
        val start = OperationRuntimeStateHolder.startedAtMillis(operationId)
            ?: OperationRuntimeStateHolder.ensureStartedAt(operationId)
        val end = OperationRuntimeStateHolder.endedAtMillis(operationId)
            ?: System.currentTimeMillis()
        val safeStart = start.takeIf { it > 0L } ?: end
        return EndCompleteSummary(
            routeName = op.routeName,
            vehicleName = op.vehicleName,
            scheduledDepart = op.departTime,
            actualStart = OperationTripClock.formatHm(safeStart),
            actualEnd = OperationTripClock.formatHm(end),
            totalDuration = OperationTripClock.formatDurationHms(safeStart, end),
        )
    }
}
