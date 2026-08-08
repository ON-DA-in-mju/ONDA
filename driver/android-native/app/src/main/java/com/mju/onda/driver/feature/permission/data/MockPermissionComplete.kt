package com.mju.onda.driver.feature.permission.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.ui.graphics.vector.ImageVector

data class PermissionStatusItem(
    val icon: ImageVector,
    val label: String,
    val statusText: String,
    val isActive: Boolean,
)

object MockPermissionComplete {
    const val HEADLINE = "권한 설정이 완료되었습니다!"
    const val SUBTITLE =
        "운행을 위한 모든 준비가 완료되었습니다.\n안전 운행에 협조해 주셔서 감사합니다."
    const val GO_TO_OPERATION_LABEL = "운행 화면으로 이동"

    const val STATUS_ALLOWED = "허용됨"
    const val STATUS_IN_USE = "사용 중"
    const val STATUS_DENIED = "미허용"

    fun statusItems(
        whenInUseLocationGranted: Boolean,
        preciseLocationGranted: Boolean,
        backgroundLocationGranted: Boolean,
        notificationGranted: Boolean,
    ): List<PermissionStatusItem> = listOf(
        PermissionStatusItem(
            icon = Icons.Rounded.LocationOn,
            label = "위치 권한",
            statusText = if (whenInUseLocationGranted) STATUS_ALLOWED else STATUS_DENIED,
            isActive = whenInUseLocationGranted,
        ),
        PermissionStatusItem(
            icon = Icons.Rounded.GpsFixed,
            label = "정확한 위치",
            statusText = if (preciseLocationGranted) STATUS_IN_USE else STATUS_DENIED,
            isActive = preciseLocationGranted,
        ),
        PermissionStatusItem(
            icon = Icons.Rounded.Layers,
            label = "백그라운드 위치",
            statusText = if (backgroundLocationGranted) STATUS_ALLOWED else STATUS_DENIED,
            isActive = backgroundLocationGranted,
        ),
        PermissionStatusItem(
            icon = Icons.Rounded.Notifications,
            label = "알림 권한",
            statusText = if (notificationGranted) STATUS_ALLOWED else STATUS_DENIED,
            isActive = notificationGranted,
        ),
    )
}
