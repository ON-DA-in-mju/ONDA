package com.mju.onda.driver.feature.alarm.data

enum class AlarmCategory {
    Operation,
    AssignmentChange,
    DepartureTimeChange,
    OperationCancel,
    /** 관리자 공지(기사 대상) */
    Notice,
}

enum class AlarmFilter {
    All,
    Unread,
    Operation,
    AssignmentChange,
    Notice,
}

data class OperationAlarm(
    val id: String,
    val title: String,
    val body: String,
    val timeLabel: String,
    val category: AlarmCategory,
    val isUnread: Boolean,
    /** 공지 상세 다이얼로그용 (관리자 미리보기와 동일 구성) */
    val noticeHeadline: String? = null,
    val noticeType: String? = null,
    val noticeDateTime: String? = null,
    val noticeContent: String? = null,
)

object MockOperationAlarms {
    const val SCREEN_TITLE = "운행 알림"

    val filters: List<Pair<AlarmFilter, String>> = listOf(
        AlarmFilter.All to "전체",
        AlarmFilter.Unread to "미확인",
        AlarmFilter.Notice to "공지",
        AlarmFilter.Operation to "운행",
        AlarmFilter.AssignmentChange to "배정 변경",
    )

    /** LocalAlarmStore에서 가져온 실제 알림 목록 */
    val seedItems: List<OperationAlarm>
        get() = LocalAlarmStore.getAll()

    val items: List<OperationAlarm>
        get() = withReadState(seedItems)

    fun withReadState(source: List<OperationAlarm> = seedItems): List<OperationAlarm> =
        source.map { it.copy(isUnread = AlarmReadStateHolder.isUnread(it.id)) }

    fun filtered(filter: AlarmFilter): List<OperationAlarm> {
        val current = items
        return when (filter) {
            AlarmFilter.All -> current
            AlarmFilter.Unread -> current.filter { it.isUnread }
            AlarmFilter.Notice -> current.filter { it.category == AlarmCategory.Notice }
            AlarmFilter.Operation -> current.filter {
                it.category == AlarmCategory.Operation ||
                    it.category == AlarmCategory.DepartureTimeChange ||
                    it.category == AlarmCategory.OperationCancel
            }
            AlarmFilter.AssignmentChange ->
                current.filter { it.category == AlarmCategory.AssignmentChange }
        }
    }

    fun hasUnread(): Boolean = AlarmReadStateHolder.hasUnread()

    fun unreadCount(): Int = AlarmReadStateHolder.unreadCount()
}
