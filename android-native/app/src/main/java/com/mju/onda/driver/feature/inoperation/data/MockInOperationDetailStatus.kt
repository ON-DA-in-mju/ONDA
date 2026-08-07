package com.mju.onda.driver.feature.inoperation.data

import com.mju.onda.driver.core.OperationTripClock
import com.mju.onda.driver.feature.home.data.MockTodayOperations
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder
import com.mju.onda.driver.feature.operation.data.MockOperationDetail

data class InOperationDetailStatusInfo(
    val id: String,
    val routeName: String,
    val vehicleName: String,
    val statusLabel: String,
    val actualStartTime: String,
    val elapsedLabel: String,
    val expectedEndTime: String,
    val origin: String,
    val destination: String,
    val lastTransmission: String,
    val networkStatus: String,
    val serverStatus: String,
    val locationBadge: String = MockInOperationDetailStatus.LOCATION_BADGE,
    val transmissionOk: Boolean = true,
)

object MockInOperationDetailStatus {
    const val SCREEN_TITLE = "상세 상태 보기"

    const val SECTION_TRANSMISSION = "전송 및 연결 상태"
    const val LOCATION_BADGE = "위치 전송 상태 | 정상 전송 중"

    const val LABEL_ACTUAL_START = "실제 시작 시간"
    const val LABEL_ELAPSED = "경과 시간"
    const val LABEL_EXPECTED_END = "예상 종료 시간"

    const val LABEL_LAST_TRANSMISSION = "마지막 전송"
    const val LABEL_NETWORK = "네트워크"
    const val LABEL_SERVER = "서버 연결"

    const val SAFETY_TITLE = "안전 운전 안내"
    const val SAFETY_BODY = "안전을 위해 운전중에는 화면을 조작하지 마세요."

    const val END_OPERATION_LABEL = "운행 종료"
    const val SUSPEND_REQUEST_LABEL = "운행 중단 요청"
    const val END_PENDING_TOAST = "운행 종료 화면은 다음 단계에서 연결합니다."
    const val SUSPEND_PENDING_TOAST = "운행 중단 요청 화면은 다음 단계에서 연결합니다."

    fun forOperationId(operationId: String): InOperationDetailStatusInfo {
        val detail = MockOperationDetail.forOperationId(operationId)
        val op = MockTodayOperations.assignedOperations.find { it.id == operationId }
        val start = OperationRuntimeStateHolder.ensureStartedAt(operationId)
            .takeIf { it > 0L }
            ?: System.currentTimeMillis()
        return InOperationDetailStatusInfo(
            id = op?.id ?: detail.id,
            routeName = op?.routeName ?: detail.routeName,
            vehicleName = op?.vehicleName ?: detail.vehicleName,
            statusLabel = "운행 중",
            actualStartTime = OperationTripClock.formatHm(start),
            elapsedLabel = OperationTripClock.formatElapsedMinutes(start),
            expectedEndTime = op?.expectedEndTime ?: detail.expectedEndTime,
            origin = op?.origin ?: detail.origin,
            destination = op?.destination ?: detail.destination,
            lastTransmission = "방금 전",
            networkStatus = "연결됨",
            serverStatus = "정상",
        )
    }
}
