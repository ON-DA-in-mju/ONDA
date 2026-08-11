package com.mju.onda.driver.feature.home.data



import com.mju.onda.driver.feature.auth.data.SessionStateHolder



enum class OperationStatus {

    Waiting,

    DepartingSoon,

    Scheduled,

    InProgress, // 운행 중

    Ended, // 운행 종료

    Unavailable, // 운행 불가 (관리자 중단, CANCELLED)

}



data class AssignedOperation(

    val id: String,

    val routeName: String,

    val vehicleName: String,

    val departTime: String,

    val origin: String,

    val destination: String,

    val round: Int,

    val expectedEndTime: String,

    val status: OperationStatus,

)



object MockTodayOperations {

    const val SCREEN_TITLE = "오늘의 운행"

    const val GREETING = "기사님, 안녕하세요"

    /** 실시간 오늘 날짜 (예: 8월 7일 금요일) */
    val DATE_LABEL: String
        get() = com.mju.onda.driver.core.OndaDates.homeDateLabel()



    const val ASSIGNED_LABEL = "오늘 배정"

    const val UNREAD_ALARM_LABEL = "읽지 않은 알림"

    const val NEXT_TRIP_BADGE = "다음 운행"

    const val IN_PROGRESS_SECTION_TITLE = "현재 운행 중"

    const val WAITING_BADGE = "운행 대기"

    const val DEPARTING_SOON_BADGE = "곧 출발"

    const val IN_PROGRESS_BADGE = "운행 중"

    const val SCHEDULED_BADGE = "운행 예정"

    const val ENDED_BADGE = "운행 종료"

    const val UNAVAILABLE_BADGE = "운행 불가"

    const val DETAIL_BUTTON = "운행 상세 보기"

    const val LIST_SECTION_TITLE = "오늘 배정된 운행"



    // DRI-01-00B 배정 없음

    const val EMPTY_TITLE = "오늘 배정된 운행이 없습니다."

    const val EMPTY_SUBTITLE = "배정 정보가 다르다면 운수회사 관리자에게 문의해주세요."

    const val EMPTY_INFO = "다음 배정이 등록되면 이 화면에서 바로 확인할 수 있어요."

    const val REFRESH_LABEL = "새로고침"

    const val CONTACT_ADMIN_LABEL = "관리자 문의"

    const val CONTACT_ADMIN_TOAST = "관리자에게 문의해 주세요."



    const val UNREAD_ALARM_COUNT = 2



    // DRI-01-02E 출발시간 임박

    const val ALERT_IMMINENT_TITLE = "출발 예정시간이 5분 남았습니다."

    const val ALERT_IMMINENT_BODY =

        "운행정보를 확인하고 출발 전에 운행 시작 버튼을\n눌러주세요."



    // DRI-01-02F 예정시간 경과_미시작

    const val ALERT_OVERDUE_TITLE = "예정된 운행이 아직 시작되지 않았습니다."

    const val ALERT_OVERDUE_BODY = "현재 운행 여부를 확인해 주세요."



    // 테스트용 — 알림 배너 표시 로직은 ViewModel에 유지 (서버 연동 시 사용)




    /** user01 · 그 외 기본 계정 */

    private val driver01Operations: List<AssignedOperation> = listOf(

        AssignedOperation(

            id = "op-0905",

            routeName = "기흥역 통학버스",

            vehicleName = "2호차",

            departTime = "09:05",

            origin = "채플관 앞",

            destination = "기흥역 5번 출구",

            round = 1,

            expectedEndTime = "09:25",

            status = OperationStatus.Scheduled,

        ),

        AssignedOperation(

            id = "op-1000",

            routeName = "명지대역 셔틀",

            vehicleName = "1호차",

            departTime = "10:00",

            origin = "자연캠퍼스",

            destination = "명지대역",

            round = 1,

            expectedEndTime = "10:25",

            status = OperationStatus.Scheduled,

        ),

        AssignedOperation(

            id = "op-1200",

            routeName = "시내 셔틀",

            vehicleName = "3호차",

            departTime = "12:00",

            origin = "채플관 앞",

            destination = "용인시청",

            round = 1,

            expectedEndTime = "12:40",

            status = OperationStatus.Scheduled,

        ),

    )



    /** user02 — 동일 형식(3건), 경로·시간·차량만 다름 */

    private val driver02Operations: List<AssignedOperation> = listOf(

        AssignedOperation(

            id = "d02-op-0840",

            routeName = "기흥역 통학버스",

            vehicleName = "1호차",

            departTime = "08:40",

            origin = "채플관 앞",

            destination = "기흥역 5번 출구",

            round = 1,

            expectedEndTime = "09:10",

            status = OperationStatus.Scheduled,

        ),

        AssignedOperation(

            id = "d02-op-1110",

            routeName = "명지대역 셔틀",

            vehicleName = "1호차",

            departTime = "11:10",

            origin = "자연캠퍼스",

            destination = "명지대역",

            round = 1,

            expectedEndTime = "11:40",

            status = OperationStatus.Scheduled,

        ),

        AssignedOperation(

            id = "d02-op-1420",

            routeName = "시내 셔틀",

            vehicleName = "4호차",

            departTime = "14:20",

            origin = "채플관 앞",

            destination = "용인시청",

            round = 1,

            expectedEndTime = "15:00",

            status = OperationStatus.Scheduled,

        ),

    )



    val assignedOperations: List<AssignedOperation>

        get() = forUser(SessionStateHolder.currentUserId)



    /** Supabase 캐시가 있으면 사용. 없으면 빈 목록 (로컬 mock 시드 사용 안 함). */
    fun forUser(userId: String?): List<AssignedOperation> {
        TodayAssignmentsHolder.getOrNull()?.let { return it }
        return emptyList()
    }

    /** 시드 템플릿 (서버 시드 SQL과 동기화용 참고). 앱 런타임에서는 사용하지 않음. */
    fun seedForUser(userId: String?): List<AssignedOperation> = when (userId) {

        "user02" -> driver02Operations

        "user03", "user04", "user05" -> emptyList()

        else -> driver01Operations // user01 및 기본

    }



    fun statusLabel(status: OperationStatus): String = when (status) {

        OperationStatus.Waiting -> WAITING_BADGE

        OperationStatus.DepartingSoon -> DEPARTING_SOON_BADGE

        OperationStatus.InProgress -> IN_PROGRESS_BADGE

        OperationStatus.Scheduled -> SCHEDULED_BADGE

        OperationStatus.Ended -> ENDED_BADGE

        OperationStatus.Unavailable -> UNAVAILABLE_BADGE

    }



    fun findById(operationId: String): AssignedOperation? =

        assignedOperations.find { it.id == operationId }



    fun requireById(operationId: String): AssignedOperation =

        findById(operationId) ?: assignedOperations.first()



    /** 예정 출발·도착으로 예상 소요(분) */

    fun estimatedDurationMinutes(departTime: String, expectedEndTime: String): Int {

        val start = parseHm(departTime) ?: return 30

        val end = parseHm(expectedEndTime) ?: return 30

        val diff = end - start

        return if (diff > 0) diff else 30

    }



    /** 예정 출발 대비 Mock 실제 운행 시각 (이력/종료 화면 공통) */

    fun mockActualTrip(scheduledDepart: String): MockActualTrip =

        when (scheduledDepart) {

            "09:05" -> MockActualTrip("09:03", "09:45", 42)

            "10:00" -> MockActualTrip("09:58", "10:38", 40)

            "12:00" -> MockActualTrip("12:02", "12:40", 38)

            "08:40" -> MockActualTrip("08:38", "09:12", 34)

            "11:10" -> MockActualTrip("11:08", "11:42", 34)

            "14:20" -> MockActualTrip("14:22", "15:05", 43)

            else -> {

                val start = parseHm(scheduledDepart) ?: return MockActualTrip(

                    scheduledDepart,

                    scheduledDepart,

                    30,

                )

                val actualStart = formatHm(start - 2)

                val actualEnd = formatHm(start + 30)

                MockActualTrip(actualStart, actualEnd, 32)

            }

        }



    private fun parseHm(value: String): Int? {

        val parts = value.split(":")

        if (parts.size != 2) return null

        val h = parts[0].toIntOrNull() ?: return null

        val m = parts[1].toIntOrNull() ?: return null

        return h * 60 + m

    }



    private fun formatHm(totalMinutes: Int): String {

        val normalized = ((totalMinutes % (24 * 60)) + (24 * 60)) % (24 * 60)

        val h = normalized / 60

        val m = normalized % 60

        return "%02d:%02d".format(h, m)

    }

}



data class MockActualTrip(

    val actualStart: String,

    val actualEnd: String,

    val durationMinutes: Int,

) {

    val durationLabel: String get() = "${durationMinutes}분"

    val timeRange: String get() = "$actualStart ~ $actualEnd"

    val durationClock: String

        get() = "00:${durationMinutes.toString().padStart(2, '0')}:00"

}

