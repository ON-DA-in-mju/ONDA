package com.mju.onda.driver.feature.adminforceend.data

import com.mju.onda.driver.core.OperationTripClock
import com.mju.onda.driver.feature.home.data.MockTodayOperations
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder

data class AdminForceEndInfo(
    val routeName: String,
    val vehicleName: String,
    val actualStart: String,
    val processedAt: String,
    val reason: String,
    val processor: String,
)

object MockAdminForceEnd {
    const val SCREEN_TITLE = "운행 종료"
    const val HEADLINE = "관리자에 의해\n운행이 종료되었습니다"
    const val SUBTITLE = "차량 위치 전송이 중단되었습니다."

    const val LABEL_ROUTE = "노선"
    const val LABEL_VEHICLE = "차량"
    const val LABEL_ACTUAL_START = "실제 시작 시간"
    const val LABEL_PROCESSED_AT = "종료 처리 시간"
    const val LABEL_REASON = "처리 사유"
    const val LABEL_PROCESSOR = "처리자"

    const val GO_TODAY_LABEL = "오늘의 운행으로"
    const val CONTACT_ADMIN_LABEL = "관리자 문의하기"
    const val CONTACT_ADMIN_TOAST = "관리자 문의는 다음 단계에서 연결합니다."
    const val NOTICE = "문의 사항이 있으시면\n관리자에게 연락해 주세요."

    fun forOperationId(operationId: String): AdminForceEndInfo {
        val op = MockTodayOperations.findById(operationId)
            ?: MockTodayOperations.assignedOperations.first()
        val start = OperationRuntimeStateHolder.startedAtMillis(operationId)
            ?: OperationRuntimeStateHolder.ensureStartedAt(operationId)
        val end = OperationRuntimeStateHolder.endedAtMillis(operationId)
            ?: System.currentTimeMillis()
        val safeStart = start.takeIf { it > 0L } ?: end
        return AdminForceEndInfo(
            routeName = op.routeName,
            vehicleName = op.vehicleName,
            actualStart = OperationTripClock.formatHm(safeStart),
            processedAt = OperationTripClock.formatHm(end),
            reason = "종료 누락",
            processor = "관리자",
        )
    }
}
