package com.mju.onda.driver.feature.permission.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.rounded.FilterNone
import androidx.compose.ui.graphics.vector.ImageVector

data class PermissionGuideItem(
    val icon: ImageVector,
    val title: String,
    val description: String,
)

object MockPermissionGuide {
    const val TITLE = "권한 안내"
    const val HEADLINE = "원활한 운행을 위해\n다음 권한이 필요합니다"
    const val SUBTITLE =
        "권한은 서비스 이용에 필요한 항목만 요청하며,\n언제든지 설정에서 변경할 수 있습니다."

    const val SETUP_LABEL = "권한 설정하기"
    const val LATER_LABEL = "나중에 설정"
    const val FOOTER_NOTICE = "필수 권한을 허용하지 않으면 운행 시작이 불가능합니다."

    const val LATER_DIALOG_TITLE = "권한 설정 안내"
    const val LATER_DIALOG_MESSAGE =
        "필수 권한이 없으면 운행을 시작할 수 없습니다. 지금 설정할까요?"
    const val LATER_DIALOG_CONFIRM = "권한 설정하기"
    const val LATER_DIALOG_DISMISS = "나중에"

    const val REQUIRED_DIALOG_TITLE = "권한이 필요합니다"
    const val REQUIRED_DIALOG_MESSAGE =
        "필수 권한을 허용해야 운행을 시작할 수 있습니다.\n설정에서 권한을 허용해 주세요."
    const val REQUIRED_DIALOG_SETTINGS = "설정으로 이동"
    const val REQUIRED_DIALOG_CLOSE = "확인"

    val items: List<PermissionGuideItem> = listOf(
        PermissionGuideItem(
            icon = Icons.Outlined.LocationOn,
            title = "정확한 위치",
            description = "차량의 현재 위치를 정확하게 확인하기 위해 필요합니다.",
        ),
        PermissionGuideItem(
            icon = Icons.Outlined.MyLocation,
            title = "앱 사용중 위치",
            description = "앱 사용중 실시간으로 위치를 전송하기 위해 필요합니다.",
        ),
        PermissionGuideItem(
            icon = Icons.Rounded.FilterNone,
            title = "백그라운드 위치",
            description = "화면이 꺼져도 위치를 지속적으로 전송하기 위해 필요합니다.",
        ),
        PermissionGuideItem(
            icon = Icons.Outlined.Notifications,
            title = "알림 권한",
            description = "운행 관련 중요 알림을 받기 위해 필요합니다.",
        ),
    )
}
