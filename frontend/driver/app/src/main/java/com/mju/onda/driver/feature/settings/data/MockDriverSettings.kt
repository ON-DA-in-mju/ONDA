package com.mju.onda.driver.feature.settings.data

enum class SettingsMenuId {
    DevicePermission,
    Alarm,
    LocationConsent,
    ContactAdmin,
    SafeStop,
}

data class SettingsMenuItem(
    val id: SettingsMenuId,
    val label: String,
)

data class DriverProfile(
    val nameLabel: String = "기사명",
    val name: String,
    val orgLabel: String = "소속",
    val organization: String,
)

object MockDriverSettings {
    const val SCREEN_TITLE = "설정"

    const val INFO_BANNER = "운행 중에는 일부 설정이\n제한될 수 있습니다."
    const val LOGOUT_LABEL = "로그아웃"

    const val PENDING_TOAST = "해당 메뉴는 다음 단계에서 연결합니다."
    const val LOGOUT_TOAST = "로그아웃되었습니다."
    const val NOT_IN_OPERATION_TOAST = "현재 차량이 운행 중이 아닙니다"

    val profile = DriverProfile(
        name = "박사용 기사님",
        organization = "명지 셔틀 운영팀",
    )

    val menuItems: List<SettingsMenuItem> = listOf(
        SettingsMenuItem(SettingsMenuId.DevicePermission, "기기 · 권한 상태"),
        SettingsMenuItem(SettingsMenuId.Alarm, "알람 설정"),
        SettingsMenuItem(SettingsMenuId.LocationConsent, "위치 정보 동의 관리"),
        SettingsMenuItem(SettingsMenuId.ContactAdmin, "관리자 문의"),
        SettingsMenuItem(SettingsMenuId.SafeStop, "안전 정차"),
    )
}
