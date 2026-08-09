package com.mju.onda.driver.feature.startcomplete.data

import com.mju.onda.driver.core.OperationTripClock
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder
import com.mju.onda.driver.feature.operation.data.MockOperationDetail

data class StartCompleteInfo(
    val routeName: String,
    val vehicleName: String,
    val actualStartTime: String,
    val origin: String,
    val destination: String,
)

object MockStartComplete {
    const val SCREEN_TITLE = "운행 시작 완료"
    const val HEADLINE = "운행이 시작되었습니다!"
    const val SUBTITLE =
        "차량 위치가 학생용 앱과 관리자 화면에 실시간으로 전송되고 있습니다."
    const val LABEL_ROUTE = "노선"
    const val LABEL_VEHICLE = "차량"
    const val LABEL_ACTUAL_START = "실제 시작 시간"
    const val LABEL_ORIGIN = "출발지"
    const val LABEL_DESTINATION = "도착지"
    const val LOCATION_STATUS_LABEL = "위치 전송 상태"
    const val LOCATION_STATUS_VALUE = "정상 전송 중"
    const val GO_OPERATION_LABEL = "운행 화면으로 이동"
    const val OPERATION_PENDING_TOAST = "운행 화면은 다음 단계에서 연결합니다."

    fun forOperationId(operationId: String): StartCompleteInfo {
        val detail = MockOperationDetail.forOperationId(operationId)
        val start = OperationRuntimeStateHolder.ensureStartedAt(operationId)
            .takeIf { it > 0L }
            ?: System.currentTimeMillis()
        return StartCompleteInfo(
            routeName = detail.routeName,
            vehicleName = detail.vehicleName,
            actualStartTime = OperationTripClock.formatHm(start),
            origin = detail.origin,
            destination = detail.destination,
        )
    }
}
