package com.mju.onda.driver.feature.vehicle.data

data class VehicleChangeInfo(
    val beforeVehicle: String,
    val afterVehicle: String,
    val routeName: String,
    val roundLabel: String,
    val scheduledTime: String,
    val origin: String,
    val destination: String,
    val changeReason: String,
    val changeTime: String,
)

object MockVehicleChange {
    const val HEADLINE = "차량이 변경되었습니다"
    const val SUBTITLE = "새로운 차량 정보를 확인해 주세요."
    const val BEFORE_LABEL = "변경 전"
    const val AFTER_LABEL = "변경 후"
    const val CONFIRM_LABEL = "확인했습니다"

    const val LABEL_ROUTE = "노선"
    const val LABEL_ROUND = "운행 회차"
    const val LABEL_SCHEDULED_TIME = "운행 예정 시간"
    const val LABEL_ORIGIN = "출발지"
    const val LABEL_DESTINATION = "도착지"

    val info = VehicleChangeInfo(
        beforeVehicle = "1호차",
        afterVehicle = "2호차",
        routeName = "기흥역 통학버스",
        roundLabel = "2회차",
        scheduledTime = "09:05",
        origin = "채플관 앞",
        destination = "기흥역 5번 출구",
        changeReason = "차량 점검으로 인한 변경",
        changeTime = "2026.07.23 08:40",
    )
}
