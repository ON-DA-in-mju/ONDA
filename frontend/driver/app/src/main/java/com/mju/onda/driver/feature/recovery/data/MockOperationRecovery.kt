package com.mju.onda.driver.feature.recovery.data

import android.content.Context
import com.mju.onda.driver.core.OperationTripClock
import com.mju.onda.driver.core.location.OperationDeviceStatus
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder
import com.mju.onda.driver.feature.operation.data.MockOperationDetail

data class OperationRecoveryInfo(
    val id: String,
    val routeName: String,
    val vehicleName: String,
    val actualStartTime: String,
    val lastTransmission: String,
    val routeSection: String,
    val transmissionLabel: String = MockOperationRecovery.TRANSMITTING_OK,
    val transmissionOk: Boolean = true,
    val showBatteryWarning: Boolean = false,
)

object MockOperationRecovery {
    const val SCREEN_TITLE = "운행 복구"
    const val HEADLINE = "진행 중인 운행이 있습니다."
    const val SUBTITLE = "앱을 다시 열었습니다. 위치 전송은 계속 진행 중입니다."
    const val BADGE_IN_PROGRESS = "진행 중"
    const val TRANSMITTING_OK = "정상 전송 중"
    const val TRANSMITTING_WARN = "전송 이상"
    const val LABEL_ACTUAL_START = "실제 시작 시간"
    const val LABEL_LAST_TRANSMISSION = "마지막 전송 시간"
    const val LABEL_ROUTE_SECTION = "운행 구간"
    const val INFO_TITLE = "화면이 꺼져도 위치 전송은 계속됩니다."
    const val INFO_BODY = "앱이 백그라운드에서도 완전하고 정확하게 운행데이터를 전송합니다"
    const val BATTERY_WARNING_TITLE = "배터리가 부족합니다. 충전기를 연결해 주세요."
    const val BATTERY_WARNING_BODY = "위치 전송은 계속 되지만 중단될 수 있습니다."
    const val GO_OPERATION_LABEL = "운행 화면으로 이동"
    const val GO_TODAY_LABEL = "오늘의 운행으로"

    fun forOperationId(context: Context, operationId: String): OperationRecoveryInfo {
        val detail = MockOperationDetail.forOperationId(operationId)
        val start = OperationRuntimeStateHolder.ensureStartedAt(operationId)
            .takeIf { it > 0L }
            ?: System.currentTimeMillis()
        val tx = OperationDeviceStatus.transmissionSnapshot(context, operationId)
        val battery = OperationDeviceStatus.batterySnapshot(context)
        return OperationRecoveryInfo(
            id = operationId.ifBlank { detail.id },
            routeName = detail.routeName,
            vehicleName = detail.vehicleName,
            actualStartTime = OperationTripClock.formatHm(start),
            lastTransmission = tx.lastTransmissionLabel,
            routeSection = "${detail.origin} → ${detail.destination}",
            transmissionLabel = if (tx.isOk) TRANSMITTING_OK else TRANSMITTING_WARN,
            transmissionOk = tx.isOk,
            showBatteryWarning = battery.needsWarning,
        )
    }
}
