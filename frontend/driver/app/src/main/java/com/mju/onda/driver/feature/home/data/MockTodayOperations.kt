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
    /** buses.vehicle_number */
    val plateNumber: String = "",
    val departTime: String,
    val origin: String,
    val destination: String,
    val round: Int,
    val expectedEndTime: String,
    val status: OperationStatus,
    /** operations.id(uuid). [id] 는 external_id 일 수 있다. */
    val dbId: String = "",
    /** DB operations.started_at. 다른 기기에서 이어받을 때 사용. */
    val startedAtMillis: Long = 0L,
    val endedAtMillis: Long = 0L,
) {
    fun matchesId(operationId: String): Boolean {
        if (operationId.isBlank()) return false
        return id == operationId || (dbId.isNotBlank() && dbId == operationId)
    }
}

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

    const val EMPTY_TITLE = "오늘 배정된 운행이 없습니다."
    const val EMPTY_SUBTITLE = "배정 정보가 다르다면 운수회사 관리자에게 문의해주세요."
    const val EMPTY_INFO = "다음 배정이 등록되면 이 화면에서 바로 확인할 수 있어요."
    const val REFRESH_LABEL = "새로고침"
    const val CONTACT_ADMIN_LABEL = "관리자 문의"
    const val CONTACT_ADMIN_TOAST = "관리자에게 문의해 주세요."

    const val ALERT_IMMINENT_TITLE = "출발 예정시간이 5분 남았습니다."
    const val ALERT_IMMINENT_BODY =
        "운행정보를 확인하고 출발 전에 운행 시작 버튼을\n눌러주세요."
    const val ALERT_OVERDUE_TITLE = "예정된 운행이 아직 시작되지 않았습니다."
    const val ALERT_OVERDUE_BODY = "현재 운행 여부를 확인해 주세요."

    val assignedOperations: List<AssignedOperation>
        get() = forUser(SessionStateHolder.currentUserId)

    /** Supabase 캐시가 있으면 사용. 없으면 빈 목록. */
    fun forUser(userId: String?): List<AssignedOperation> {
        TodayAssignmentsHolder.getOrNull()?.let { return it }
        return emptyList()
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
        assignedOperations.find { it.matchesId(operationId) }

    fun requireById(operationId: String): AssignedOperation =
        findById(operationId)
            ?: AssignedOperation(
                id = operationId,
                routeName = "",
                vehicleName = "",
                plateNumber = "",
                departTime = "",
                origin = "",
                destination = "",
                round = 1,
                expectedEndTime = "",
                status = OperationStatus.Scheduled,
                dbId = operationId,
            )

    /** 예정 출발·도착으로 예상 소요(분) */
    fun estimatedDurationMinutes(departTime: String, expectedEndTime: String): Int {
        val start = parseHm(departTime) ?: return 30
        val end = parseHm(expectedEndTime) ?: return 30
        val diff = end - start
        return if (diff > 0) diff else 30
    }

    private fun parseHm(value: String): Int? {
        val parts = value.split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        return h * 60 + m
    }
}
