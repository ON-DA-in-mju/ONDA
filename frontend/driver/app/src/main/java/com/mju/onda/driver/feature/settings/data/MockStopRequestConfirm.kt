package com.mju.onda.driver.feature.settings.data

object MockStopRequestConfirm {
    const val SCREEN_TITLE = "중단 요청 확인"
    const val HEADLINE = "운행 중단 요청을\n전송하시겠습니까?"
    const val SUBTITLE =
        "요청은 관리자에게 전달되며\n확인 후 학생용 앱과 공식 공지에 반영됩니다."

    const val LABEL_REASON = "사유"
    const val LABEL_ROUTE = "노선"
    const val LABEL_VEHICLE = "차량"
    const val LABEL_LOCATION = "현재 위치"
    const val LABEL_ATTACHMENT = "첨부 상태"

    const val LOCATION_FALLBACK = "채플관 앞 인근"
    const val LOCATION_NOT_SHARED = "위치 미첨부"
    const val ATTACHMENT_WITH_LOCATION = "현재 위치 포함"
    const val ATTACHMENT_WITHOUT_LOCATION = "위치 미포함"

    const val SEND_LABEL = "요청 전송"
    const val CANCEL_LABEL = "취소"
    const val SEND_TOAST = "중단 요청이 전송되었습니다."
}
