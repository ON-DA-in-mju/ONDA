package com.mju.onda.driver.feature.settings.data

object MockStopRequestDetail {
    const val SCREEN_TITLE = "중단 요청 상세"
    const val STATUS_LABEL = "현재 상태"
    const val STATUS_IN_PROGRESS = "운행 중"
    const val SELECTED_REASON_PREFIX = "선택한 사유 : "
    const val MESSAGE_LABEL = "관리자에게 전달할 내용"
    const val MESSAGE_REQUIRED_HINT = "(필수 / 10자이상)"
    const val MESSAGE_HINT = "상세 내용을 입력해 주세요."
    const val MAX_MESSAGE_LENGTH = 300
    const val MIN_MESSAGE_LENGTH = 10

    const val LOCATION_TITLE = "현재 위치 자동 첨부"
    const val LOCATION_DESC = "중단 요청 시 현재 위치가 자동으로 공유됩니다."
    const val CONTACT_TITLE = "연락 가능 상태"
    const val CONTACT_DESC = "관리자가 연락할 수 있도록 번호가 공유됩니다."
    const val REQUIRED_LABEL = "(필수)"

    const val INFO_BANNER = "입력 내용은 관리자와 운영 화면에 전달됩니다."
    const val SUBMIT_LABEL = "중단 요청 전송"
    const val PREV_LABEL = "이전"
    const val LOCATION_FALLBACK = "채플관 앞 인근"
}
