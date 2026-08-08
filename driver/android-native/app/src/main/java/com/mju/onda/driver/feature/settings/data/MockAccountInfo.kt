package com.mju.onda.driver.feature.settings.data

data class AccountInfo(
    val driverNameLabel: String = "기사명",
    val driverName: String,
    val driverIdLabel: String = "기사 ID",
    val driverId: String,
    val orgLabel: String = "소속",
    val organization: String,
    val vehicleLabel: String = "담당 차량",
    val vehicleName: String,
    val contactLabel: String = "연락 상태",
    val contactStatus: String,
)

object MockAccountInfo {
    const val SCREEN_TITLE = "계정 정보"
    const val EDIT_SCREEN_TITLE = "계정 정보 수정"

    const val INFO_BANNER = "수정하기 버튼을 통해 잘못 입력된 정보를 수정해주세요."
    const val EDIT_BANNER = "잘못된 정보가 있으면 수정 후 저장해 주세요.\n기사 ID는 변경할 수 없습니다."
    const val EDIT_LABEL = "수정하기"
    const val SAVE_LABEL = "저장하기"
    const val CANCEL_LABEL = "취소"
    const val GO_SETTINGS_LABEL = "설정으로"
    const val SAVE_TOAST = "계정 정보가 저장되었습니다."
    const val NAME_SUFFIX = " 기사님"
    const val NAME_HINT = "이름"

    val info = AccountInfo(
        driverName = "김민수 기사님",
        driverId = "driver_mju_023",
        organization = "명지 셔틀 운영팀",
        vehicleName = "2호차",
        contactStatus = "관리자 문의를 통해 연결",
    )

    fun extractGivenName(displayName: String): String =
        displayName.removeSuffix(NAME_SUFFIX.trimStart()).removeSuffix("기사님").trim()

    fun formatDisplayName(givenName: String): String {
        val name = givenName.trim().ifBlank { extractGivenName(info.driverName) }
        return "$name$NAME_SUFFIX"
    }
}
