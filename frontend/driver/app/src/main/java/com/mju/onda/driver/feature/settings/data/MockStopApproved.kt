package com.mju.onda.driver.feature.settings.data

object MockStopApproved {
    const val SCREEN_TITLE = "중단 승인"
    const val HEADLINE_PREFIX = "운행 중단이 "
    const val HEADLINE_HIGHLIGHT = "승인"
    const val HEADLINE_SUFFIX = "되었습니다."
    const val SUBTITLE = "학생용 앱과 관리자 화면에\n운행 중단 상태가 반영되었습니다."

    const val LABEL_TIME = "승인 시각"
    const val LABEL_REASON = "중단 사유"
    const val LABEL_LOCATION = "위치 전송 상태"
    const val LABEL_ADMIN = "관리자 안내"

    const val LOCATION_ENDED = "종료됨"
    const val ADMIN_GUIDE = "안전한 장소에서 대기 후\n후속 안내를 확인해 주세요."
    const val SAFETY_BANNER =
        "안전을 최우선으로 해주세요.\n차량을 안전한 장소에 정차하고,\n학생들의 안전을 위해 안내를 확인해 주세요."

    const val END_OPERATION_LABEL = "운행 종료하기"
    const val CONTACT_ADMIN_LABEL = "관리자 문의"
}

object MockContinueOperation {
    const val SCREEN_TITLE = "계속 운행 요청"
    const val HEADLINE = "관리자가 계속 운행을 요청했습니다."
    const val SUBTITLE = "현장 상황을 확인한 후\n운행을 계속 진행해 주세요."

    const val LABEL_TIME = "안내 시각"
    const val LABEL_ADMIN = "관리자 안내"
    const val LABEL_STATUS = "현재 상태"
    const val LABEL_LOCATION = "위치 전송 상태"

    const val ADMIN_MESSAGE = "현장 확인 결과 운행이 가능하여\n계속 운행 요청드립니다."
    const val STATUS_WAITING = "대기"
    const val LOCATION_NORMAL = "정상"
    const val NOTICE = "운행을 재개하기 전 차량 상태를 다시 확인해 주세요."

    const val CONTINUE_LABEL = "운행 계속"
    const val CONTACT_ADMIN_LABEL = "관리자 연락"
}
