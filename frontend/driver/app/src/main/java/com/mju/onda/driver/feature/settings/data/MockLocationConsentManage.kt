package com.mju.onda.driver.feature.settings.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.ui.graphics.vector.ImageVector
import com.mju.onda.driver.feature.consent.data.LocationConsentPrefs

data class ConsentStatusRow(
    val id: String,
    val label: String,
    val value: String,
    val icon: ImageVector,
)

object MockLocationConsentManage {
    const val SCREEN_TITLE = "위치정보 동의 관리"
    const val SECTION_TITLE = "현재 동의 상태"
    const val STATUS_BADGE = "동의 중"
    const val STATUS_BADGE_DENIED = "미동의"

    const val INFO_LINE_1 = "운행 외 시간에는 위치를 수집하지 않습니다"
    const val INFO_LINE_2 = "자세한 내용은 개인정보 처리방침에서 확인할 수 있습니다."

    const val PRIVACY_POLICY_LABEL = "개인정보 처리방침"
    const val CONSENT_GUIDE_LABEL = "동의 안내 보기"
    const val AGREE_LABEL = "동의하기"
    const val REVOKE_LABEL = "동의 철회"
    const val DIALOG_CONFIRM = "확인"
    const val DIALOG_CANCEL = "취소"

    const val AGREE_DIALOG_TITLE = "위치정보 이용 동의"
    const val AGREE_DIALOG_MESSAGE =
        "운행 중 차량 위치 수집·이용에 동의합니다.\n동의 후에도 설정에서 언제든지 철회할 수 있습니다."
    const val AGREE_DIALOG_CONFIRM = "동의합니다"

    const val REVOKE_DIALOG_TITLE = "동의를 철회할까요?"
    const val REVOKE_DIALOG_MESSAGE =
        "동의를 철회하면 운행 위치 공유 기능 이용이 제한될 수 있습니다.\n다시 이용하려면 이 화면에서 다시 동의해 주세요."
    const val REVOKE_DIALOG_CONFIRM = "철회하기"

    const val TOAST_AGREED = "위치정보 이용에 동의했습니다."
    const val TOAST_REVOKED = "위치정보 이용 동의를 철회했습니다."

    const val PRIVACY_POLICY_BODY =
        "ON-DA는 안전한 셔틀 운행을 위해 운행 구간의 차량 위치 정보만 수집·이용합니다.\n\n" +
            "수집된 위치 정보는 학생용 앱의 실시간 위치 안내와 관리자 관제에 사용되며, " +
            "운행 외 시간에는 위치를 수집하지 않습니다.\n\n" +
            "자세한 처리 기준과 보관 기간은 서비스 운영 정책에 따릅니다."

    const val CONSENT_GUIDE_BODY =
        "위치정보는 운행 시작부터 종료까지 차량 위치만 수집합니다.\n\n" +
            "학생 앱에는 차량 위치만 표시되며, 기사님의 이름·연락처 등 개인정보는 노출되지 않습니다.\n\n" +
            "동의는 이 화면에서 언제든지 하거나 철회할 수 있습니다."

    fun rows(): List<ConsentStatusRow> = listOf(
        ConsentStatusRow(
            id = "purpose",
            label = "수집 목적",
            value = "셔틀 위치 공유 및 운행 관리",
            icon = Icons.Outlined.MyLocation,
        ),
        ConsentStatusRow(
            id = "collect_at",
            label = "수집 시점",
            value = "운행 시작 시",
            icon = Icons.Outlined.CalendarMonth,
        ),
        ConsentStatusRow(
            id = "end_at",
            label = "종료 시점",
            value = "운행 종료 시",
            icon = Icons.Outlined.Flag,
        ),
        ConsentStatusRow(
            id = "scope",
            label = "공개 범위",
            value = "학생 앱에는 차량 위치만 표시",
            icon = Icons.Outlined.Groups,
        ),
        ConsentStatusRow(
            id = "consented_at",
            label = "동의 일시",
            value = LocationConsentPrefs.formatConsentedAt(),
            icon = Icons.Outlined.AccessTime,
        ),
    )
}
