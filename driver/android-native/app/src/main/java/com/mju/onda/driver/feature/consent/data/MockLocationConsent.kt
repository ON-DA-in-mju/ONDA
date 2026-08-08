package com.mju.onda.driver.feature.consent.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.ui.graphics.vector.ImageVector

data class LocationConsentItem(
    val icon: ImageVector,
    val title: String,
    val description: String,
)

object MockLocationConsent {
    const val TITLE = "위치정보 이용 안내"
    const val HEADLINE = "안전한 셔틀버스 운행을 위해\n위치정보를 수집합니다"
    const val SUBTITLE = "정확한 운행 안내와 안전한 서비스 제공을 위한 필수 정보입니다."

    const val AGREE_LABEL = "동의하고 계속"
    const val DISAGREE_LABEL = "동의하지 않음"
    const val DETAIL_LABEL = "자세히 보기"

    const val DETAIL_BODY =
        "ON-DA 기사님용 앱은 운행 시작부터 종료까지의 차량 위치만 수집합니다.\n\n" +
            "수집 정보는 학생용 앱의 실시간 위치 안내와 관리자 관제에 사용되며, " +
            "기사님의 이름·연락처 등 개인정보는 학생 화면에 노출되지 않습니다.\n\n" +
            "동의는 설정 메뉴에서 언제든지 철회할 수 있습니다. " +
            "철회 시 위치 공유 기능 이용이 제한될 수 있습니다."

    const val DISAGREE_MESSAGE =
        "위치정보 이용에 동의하지 않으면 운행 위치 공유 기능을 사용할 수 없습니다."

    val items: List<LocationConsentItem> = listOf(
        LocationConsentItem(
            icon = Icons.Rounded.LocationOn,
            title = "위치정보 수집 목적",
            description = "학생에게 차량 위치를 제공하고\n안전한 운행 관리를 위해 사용됩니다.",
        ),
        LocationConsentItem(
            icon = Icons.Outlined.Schedule,
            title = "수집 시간",
            description = "운행 시작 버튼을 누른 시점부터\n운행 종료 시점까지만 수집합니다.",
        ),
        LocationConsentItem(
            icon = Icons.Rounded.GpsFixed,
            title = "수집 범위",
            description = "운행 중에만 수집하며,\n운행 외 시간에는 수집하지 않습니다.",
        ),
        LocationConsentItem(
            icon = Icons.Outlined.Visibility,
            title = "공개 범위",
            description = "학생에게는 차량 번호와 차량 위치만 표시되며,\n기사님의 개인정보는 표시되지 않습니다.",
        ),
        LocationConsentItem(
            icon = Icons.Rounded.VerifiedUser,
            title = "동의 철회",
            description = "위치정보 이용 동의는 언제든지 철회할 수 있으며,\n철회 시 서비스 이용이 제한될 수 있습니다.",
        ),
    )
}
