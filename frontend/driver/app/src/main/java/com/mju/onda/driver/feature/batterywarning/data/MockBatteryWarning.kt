package com.mju.onda.driver.feature.batterywarning.data

import android.content.Context
import com.mju.onda.driver.core.location.OperationDeviceStatus
import com.mju.onda.driver.feature.home.data.MockTodayOperations
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder
import com.mju.onda.driver.feature.operation.data.MockOperationDetail

enum class BatteryDiagTone {
    Warning,
    Ok,
}

data class BatteryDiagItem(
    val label: String,
    val value: String,
    val tone: BatteryDiagTone,
    val iconKind: BatteryDiagIcon,
)

enum class BatteryDiagIcon {
    Battery,
    Charging,
    PowerSave,
    Location,
}

data class BatteryWarningInfo(
    val routeName: String,
    val vehicleName: String,
    val items: List<BatteryDiagItem>,
    val bannerTitle: String = MockBatteryWarning.BANNER_TITLE,
    val isAttentionNeeded: Boolean = true,
)

object MockBatteryWarning {
    const val SCREEN_TITLE = "배터리 경고"
    const val BANNER_TITLE = "배터리가 부족합니다. 충전기를 연결해 주세요."
    const val BANNER_TITLE_POWER_SAVE = "절전 모드가 켜져 있습니다. 해제해 주세요."
    const val BANNER_TITLE_OK = "배터리·충전 상태가 안정적입니다."
    const val BANNER_BODY = "위치 전송은 계속 되지만 중단될 수 있습니다."
    const val BADGE_IN_OPERATION = "운행 중"
    const val SECTION_DIAGNOSIS = "차량 상태 진단"
    const val TIP_BODY = "안정적인 위치 전송을 위해\n차량 충전기에 연결하고 절전모드를\n해제해 주세요."
    const val CONFIRM_LABEL = "충전기 연결 확인"
    const val CLOSE_LABEL = "닫기"
    const val VALUE_CHARGING_DISCONNECTED = "연결 안 됨"
    const val VALUE_CHARGING_CONNECTED = "연결됨"
    const val VALUE_POWER_SAVE_ON = "사용 중"
    const val VALUE_POWER_SAVE_OFF = "해제됨"
    const val RECHECK_STILL_ISSUES = "아직 충전 중이 아니거나 절전 모드가 켜져 있습니다."
    const val RECHECK_OK = "충전·절전 상태가 정상입니다."
    const val LOCATION_TX_OK = "정상 전송 중"
    const val LOCATION_TX_WARN = "전송 이상"

    fun forOperationId(context: Context, operationId: String): BatteryWarningInfo {
        val detail = MockOperationDetail.forOperationId(operationId)
        val battery = OperationDeviceStatus.batterySnapshot(context)
        val tx = OperationDeviceStatus.transmissionSnapshot(context, operationId)

        val batteryTone = when {
            battery.percent < 0 -> BatteryDiagTone.Warning
            battery.percent < 30 && !battery.isCharging -> BatteryDiagTone.Warning
            else -> BatteryDiagTone.Ok
        }
        val chargingTone =
            if (battery.isCharging) BatteryDiagTone.Ok else BatteryDiagTone.Warning
        val powerTone =
            if (battery.isPowerSave) BatteryDiagTone.Warning else BatteryDiagTone.Ok
        val locationTone =
            if (tx.isOk) BatteryDiagTone.Ok else BatteryDiagTone.Warning

        val attention = battery.needsWarning
        val bannerTitle = when {
            battery.percent in 0..29 && !battery.isCharging -> BANNER_TITLE
            battery.isPowerSave -> BANNER_TITLE_POWER_SAVE
            else -> BANNER_TITLE_OK
        }

        return BatteryWarningInfo(
            routeName = detail.routeName,
            vehicleName = detail.vehicleName,
            bannerTitle = bannerTitle,
            isAttentionNeeded = attention,
            items = listOf(
                BatteryDiagItem(
                    label = "배터리",
                    value = if (battery.percent >= 0) "${battery.percent}%" else "확인 불가",
                    tone = batteryTone,
                    iconKind = BatteryDiagIcon.Battery,
                ),
                BatteryDiagItem(
                    label = "충전 상태",
                    value = if (battery.isCharging) {
                        VALUE_CHARGING_CONNECTED
                    } else {
                        VALUE_CHARGING_DISCONNECTED
                    },
                    tone = chargingTone,
                    iconKind = BatteryDiagIcon.Charging,
                ),
                BatteryDiagItem(
                    label = "절전모드",
                    value = if (battery.isPowerSave) {
                        VALUE_POWER_SAVE_ON
                    } else {
                        VALUE_POWER_SAVE_OFF
                    },
                    tone = powerTone,
                    iconKind = BatteryDiagIcon.PowerSave,
                ),
                BatteryDiagItem(
                    label = "위치 전송 상태",
                    value = if (tx.isOk) LOCATION_TX_OK else LOCATION_TX_WARN,
                    tone = locationTone,
                    iconKind = BatteryDiagIcon.Location,
                ),
            ),
        )
    }

    fun forFocusedOperation(context: Context): BatteryWarningInfo =
        forOperationId(context, OperationRuntimeStateHolder.resolveFocusedOperationId())

    val giheung: BatteryWarningInfo
        get() = BatteryWarningInfo(
            routeName = MockTodayOperations.assignedOperations.first().routeName,
            vehicleName = MockTodayOperations.assignedOperations.first().vehicleName,
            items = emptyList(),
        )
}
