package com.mju.onda.driver.feature.settings.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.SatelliteAlt
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import com.mju.onda.driver.core.location.OperationDeviceStatus

enum class DeviceStatusStyle {
    Plain, // 기기명 등 일반 텍스트
    Success, // 체크 + 초록
    Accent, // 사용 중 (민트, 체크 없음)
    Warning, // 경고 주황
    Denied, // 거부됨
}

data class DeviceStatusItem(
    val id: String,
    val label: String,
    val value: String,
    val style: DeviceStatusStyle,
    val icon: ImageVector,
)

object MockDevicePermission {
    const val SCREEN_TITLE = "기기 · 권한 상태"

    const val INFO_BANNER =
        "위치 전송이 안정적으로 동작하도록\n주기적으로 상태를 확인해 주세요."

    const val REFRESH_LABEL = "상태 다시 확인"
    const val OPEN_SETTINGS_LABEL = "설정 열기"
    const val REFRESH_TOAST = "상태를 다시 확인했습니다."

    const val NOTIFICATION_GRANTED = "허용됨"
    const val NOTIFICATION_DENIED = "거부됨"

    /** 실제 기기·권한 상태로 목록을 구성한다. */
    fun items(context: Context, notificationsEnabled: Boolean): List<DeviceStatusItem> {
        val fineGranted = hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseGranted = hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        val locationGranted = fineGranted || coarseGranted
        val backgroundGranted = OperationDeviceStatus.hasBackgroundLocationPermission(context)
        val gpsOn = OperationDeviceStatus.isGpsEnabled(context)
        val batteryExempt = isBatteryOptimizationExempt(context)

        val (preciseValue, preciseStyle) = when {
            fineGranted -> "사용 중" to DeviceStatusStyle.Accent
            coarseGranted -> "대략적만" to DeviceStatusStyle.Warning
            else -> "미사용" to DeviceStatusStyle.Denied
        }

        return listOf(
            DeviceStatusItem(
                id = "device",
                label = "현재 기기",
                value = deviceDisplayName(),
                style = DeviceStatusStyle.Plain,
                icon = Icons.Outlined.PhoneAndroid,
            ),
            DeviceStatusItem(
                id = "registered",
                label = "기기 등록 상태",
                // 서버 미연동 MVP — 앱 설치·로그인 기준으로 로컬 등록으로 표시
                value = "등록 완료",
                style = DeviceStatusStyle.Success,
                icon = Icons.Outlined.AssignmentTurnedIn,
            ),
            DeviceStatusItem(
                id = "location",
                label = "위치 권한",
                value = if (locationGranted) "허용됨" else "거부됨",
                style = if (locationGranted) DeviceStatusStyle.Success else DeviceStatusStyle.Denied,
                icon = Icons.Outlined.Place,
            ),
            DeviceStatusItem(
                id = "precise",
                label = "정확한 위치",
                value = preciseValue,
                style = preciseStyle,
                icon = Icons.Outlined.MyLocation,
            ),
            DeviceStatusItem(
                id = "background",
                label = "백그라운드 위치",
                value = if (backgroundGranted) "허용됨" else "거부됨",
                style = if (backgroundGranted) DeviceStatusStyle.Success else DeviceStatusStyle.Denied,
                icon = Icons.Outlined.Radar,
            ),
            DeviceStatusItem(
                id = "gps",
                label = "GPS 상태",
                value = if (gpsOn) "켜짐" else "꺼짐",
                style = if (gpsOn) DeviceStatusStyle.Success else DeviceStatusStyle.Denied,
                icon = Icons.Outlined.SatelliteAlt,
            ),
            DeviceStatusItem(
                id = "alarm",
                label = "알림 권한",
                value = if (notificationsEnabled) NOTIFICATION_GRANTED else NOTIFICATION_DENIED,
                style = if (notificationsEnabled) DeviceStatusStyle.Success else DeviceStatusStyle.Denied,
                icon = Icons.Outlined.Notifications,
            ),
            DeviceStatusItem(
                id = "battery",
                label = "배터리 최적화",
                value = if (batteryExempt) "예외 적용됨" else "예외 설정 권장",
                style = if (batteryExempt) DeviceStatusStyle.Success else DeviceStatusStyle.Warning,
                icon = Icons.Outlined.BatteryChargingFull,
            ),
        )
    }

    private fun deviceDisplayName(): String {
        val manufacturer = Build.MANUFACTURER.orEmpty().trim()
        val model = Build.MODEL.orEmpty().trim()
        return when {
            manufacturer.isBlank() && model.isBlank() -> Build.DEVICE.ifBlank { "알 수 없음" }
            manufacturer.isBlank() -> model
            model.isBlank() -> manufacturer.replaceFirstChar { it.uppercase() }
            model.startsWith(manufacturer, ignoreCase = true) -> model
            else -> "${manufacturer.replaceFirstChar { it.uppercase() }} $model"
        }
    }

    private fun isBatteryOptimizationExempt(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(context.packageName)
        } catch (_: Throwable) {
            false
        }
    }

    private fun hasPermission(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
