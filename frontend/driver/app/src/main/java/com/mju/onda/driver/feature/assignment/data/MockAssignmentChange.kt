package com.mju.onda.driver.feature.assignment.data

data class AssignmentChangeInfo(
    val routeName: String,
    val vehicleName: String,
    val roundLabel: String,
    val departTime: String,
    val isDepartTimeChanged: Boolean,
    val origin: String,
    val destination: String,
    val changeReason: String,
    val changeTime: String,
)

object MockAssignmentChange {
    const val HEADLINE = "배정 정보가 변경되었습니다"
    const val SUBTITLE = "새로운 운행 정보를 확인해주세요."
    const val SECTION_TITLE = "변경된 운행 정보"
    const val CONFIRM_LABEL = "변경된 정보 확인"
    const val CHANGED_BADGE = "변경됨"

    const val LABEL_ROUTE = "노선"
    const val LABEL_VEHICLE = "차량"
    const val LABEL_ROUND = "운행 회차"
    const val LABEL_DEPART_TIME = "출발 예정 시간"
    const val LABEL_ORIGIN = "출발지"
    const val LABEL_DESTINATION = "도착지"

    val info = AssignmentChangeInfo(
        routeName = "기흥역 통학버스",
        vehicleName = "2호차",
        roundLabel = "2회차",
        departTime = "10:30",
        isDepartTimeChanged = true,
        origin = "채플관 앞",
        destination = "기흥역 5번 출구",
        changeReason = "수요 조정에 따른 배정 변경",
        changeTime = "2026.07.23 08:40",
    )
}
