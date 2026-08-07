package com.mju.onda.driver.feature.inoperation.data

import com.mju.onda.driver.core.OperationTripClock
import com.mju.onda.driver.feature.home.data.MockTodayOperations
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder
import com.mju.onda.driver.feature.operation.data.MockOperationDetail

data class InOperationMinimalInfo(
    val id: String,
    val routeName: String,
    val vehicleName: String,
    val origin: String,
    val destination: String,
    val elapsedMinutes: Int,
    val actualStartTime: String,
    val lastTransmissionLabel: String,
    val locationStatusLabel: String = MockInOperationMinimal.LOCATION_OK,
    val transmissionOk: Boolean = true,
)

object MockInOperationMinimal {
    const val SCREEN_TITLE = "운행 중"
    const val HEADLINE = "운행 중"
    const val ELAPSED_SUFFIX = "분 경과"
    const val LOCATION_OK = "위치 전송 정상"
    const val LABEL_ACTUAL_START = "실제 시작"
    const val LABEL_LAST_TRANSMISSION = "마지막 전송"
    const val SAFETY_TITLE = "안전 운전 안내"
    const val SAFETY_BODY = "운전 중에는 화면을 조작하지 마세요."
    const val DETAIL_STATUS_LABEL = "상세 상태 보기"
    const val END_OPERATION_LABEL = "운행 종료"
    const val DETAIL_PENDING_TOAST = "상세 상태 화면은 다음 단계에서 연결합니다."
    const val END_PENDING_TOAST = "운행 종료 화면은 다음 단계에서 연결합니다."

    fun forOperationId(operationId: String): InOperationMinimalInfo {
        val detail = MockOperationDetail.forOperationId(operationId)
        val op = MockTodayOperations.assignedOperations.find { it.id == operationId }
        val start = OperationRuntimeStateHolder.ensureStartedAt(operationId)
            .takeIf { it > 0L }
            ?: System.currentTimeMillis()
        return InOperationMinimalInfo(
            id = op?.id ?: detail.id,
            routeName = op?.routeName ?: detail.routeName,
            vehicleName = op?.vehicleName ?: detail.vehicleName,
            origin = op?.origin ?: detail.origin,
            destination = op?.destination ?: detail.destination,
            elapsedMinutes = OperationTripClock.elapsedMinutes(start),
            actualStartTime = OperationTripClock.formatHm(start),
            lastTransmissionLabel = "방금 전",
        )
    }
}
