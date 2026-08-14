package com.mju.onda.driver.feature.endconfirm.data

import com.mju.onda.driver.core.OperationTripClock
import com.mju.onda.driver.feature.home.data.MockTodayOperations
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder

data class EndOperationConfirmInfo(
    val id: String,
    val routeName: String,
    val vehicleName: String,
    val statusLabel: String,
    val actualStartTime: String,
    val elapsedTime: String,
)

object MockEndOperationConfirm {
    const val SCREEN_TITLE = "운행 중"
    const val CONFIRM_TITLE = "운행을 종료하시겠습니까?"
    const val CONFIRM_BODY = "종료하면 차량 위치 전송이 즉시 중단됩니다."
    const val END_LABEL = "운행 종료"
    const val LABEL_ACTUAL_START = "실제 시작 시간"
    const val LABEL_ELAPSED = "운행 경과 시간"
    const val BADGE_IN_PROGRESS = "운행 중"

    const val DIALOG_TITLE = "운행 종료 확인"
    const val DIALOG_MESSAGE = "정말 운행을 종료하시겠습니까?"
    const val DIALOG_CONFIRM = "예"
    const val DIALOG_DISMISS = "아니오"

    fun forOperationId(operationId: String): EndOperationConfirmInfo {
        val op = MockTodayOperations.findById(operationId)
            ?: MockTodayOperations.assignedOperations.first()
        val start = OperationRuntimeStateHolder.ensureStartedAt(operationId)
            .takeIf { it > 0L }
            ?: System.currentTimeMillis()
        return EndOperationConfirmInfo(
            id = op.id,
            routeName = op.routeName,
            vehicleName = op.vehicleName,
            statusLabel = "운행 중",
            actualStartTime = OperationTripClock.formatHm(start),
            elapsedTime = OperationTripClock.formatDurationHms(start),
        )
    }
}
