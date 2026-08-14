package com.mju.onda.driver.feature.cancel.data

data class OperationCancelInfo(
    val routeName: String,
    val vehicleName: String,
    val roundLabel: String,
    val departTime: String,
    val origin: String,
    val destination: String,
    val cancelReason: String,
    val cancelTime: String,
)

object MockOperationCancel {
    const val HEADLINE = "운행이 취소되었습니다"
    const val SUBTITLE = "이용에 참고해 주세요."
    const val CONFIRM_LABEL = "확인"

    const val LABEL_ROUTE = "노선"
    const val LABEL_VEHICLE = "차량"
    const val LABEL_ROUND = "운행 회차"
    const val LABEL_DEPART_TIME = "출발 예정 시간"
    const val LABEL_ORIGIN = "출발지"
    const val LABEL_DESTINATION = "도착지"

    val info = OperationCancelInfo(
        routeName = "기흥역 통학버스",
        vehicleName = "2호차",
        roundLabel = "2회차",
        departTime = "09:05",
        origin = "채플관 앞",
        destination = "기흥역 5번 출구",
        cancelReason = "기상 악화 (폭우 경보)",
        cancelTime = "2026.07.23 08:30",
    )
}
