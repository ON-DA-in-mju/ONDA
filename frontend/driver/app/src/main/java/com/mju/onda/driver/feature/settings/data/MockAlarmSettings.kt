package com.mju.onda.driver.feature.settings.data



data class AlarmSettingItem(

    val id: String,

    val label: String,

    val enabled: Boolean = true,

)



object MockAlarmSettings {

    const val SCREEN_TITLE = "알림 설정"

    const val MASTER_LABEL = "알림 설정"

    const val STATUS_ON = "켜짐"

    const val STATUS_OFF = "꺼짐"

    const val INFO_BANNER =

        "기기 알림이 꺼져 있으면 아래 개별 알림을\n설정할 수 없습니다."

    const val SAVE_LABEL = "저장"

    const val GO_SETTINGS_LABEL = "목록으로"

    const val SAVE_TOAST = "알림 설정이 저장되었습니다."



    val defaults: List<AlarmSettingItem> = listOf(

        AlarmSettingItem(id = "start_soon", label = "운행 시작 임박"),

        AlarmSettingItem(id = "not_started", label = "운행 미시작 안내"),

        AlarmSettingItem(id = "end_confirm", label = "종료 확인 안내"),

        AlarmSettingItem(id = "assignment_change", label = "배정 변경 알림"),

        AlarmSettingItem(id = "cancel", label = "운행 취소 알림"),

        AlarmSettingItem(id = "stop_result", label = "중단 요청 결과"),

        AlarmSettingItem(id = "gps_network", label = "GPS · 네트워크 이상"),

    )

}

