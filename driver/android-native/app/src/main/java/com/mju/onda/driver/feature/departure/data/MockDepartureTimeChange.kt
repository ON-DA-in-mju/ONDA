package com.mju.onda.driver.feature.departure.data

data class DepartureTimeChangeInfo(
    val beforeTime: String,
    val afterTime: String,
    val routeName: String,
    val vehicleName: String,
    val roundLabel: String,
    val origin: String,
    val destination: String,
    val changeReason: String,
    val changeTime: String,
)

object MockDepartureTimeChange {
    const val HEADLINE = "출발 시간이 변경되었습니다"
    const val SUBTITLE = "변경된 시간을 확인해 주세요."
    const val BEFORE_LABEL = "변경 전"
    const val AFTER_LABEL = "변경 후"
    const val CONFIRM_LABEL = "확인"

    const val LABEL_ROUTE = "노선"
    const val LABEL_VEHICLE = "차량"
    const val LABEL_ROUND = "운행 회차"
    const val LABEL_ORIGIN = "출발지"
    const val LABEL_DESTINATION = "도착지"

    val info = DepartureTimeChangeInfo(
        beforeTime = "09:05",
        afterTime = "09:25",
        routeName = "기흥역 통학버스",
        vehicleName = "2호차",
        roundLabel = "2회차",
        origin = "채플관 앞",
        destination = "기흥역 5번 출구",
        changeReason = "교통 상황으로 인한 변경",
        changeTime = "2026.07.23 08:45",
    )
}
