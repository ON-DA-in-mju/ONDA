package com.mju.onda.driver.feature.alarm.data



import com.mju.onda.driver.feature.auth.data.SessionStateHolder



enum class AlarmCategory {

    Operation, // 운행

    AssignmentChange, // 배정 변경

    DepartureTimeChange, // 출발시간 변경

    OperationCancel, // 운행 취소

}



enum class AlarmFilter {

    All, // 전체

    Unread, // 미확인

    Operation, // 운행

    AssignmentChange, // 배정 변경

}



data class OperationAlarm(

    val id: String,

    val title: String,

    val body: String,

    val timeLabel: String,

    val category: AlarmCategory,

    val isUnread: Boolean,

)



object MockOperationAlarms {

    const val SCREEN_TITLE = "운행 알림"



    val filters: List<Pair<AlarmFilter, String>> = listOf(

        AlarmFilter.All to "전체",

        AlarmFilter.Unread to "미확인",

        AlarmFilter.Operation to "운행",

        AlarmFilter.AssignmentChange to "배정 변경",

    )



    private val driver01SeedItems: List<OperationAlarm> = listOf(

        OperationAlarm(

            id = "alarm-1",

            title = "출발시간 임박",

            body = "기흥역 통학버스 2호차 운행이 10분 후 시작됩니다.",

            timeLabel = "오전 8:55",

            category = AlarmCategory.Operation,

            isUnread = true,

        ),

        OperationAlarm(

            id = "alarm-2",

            title = "운행 미시작 확인",

            body = "09:05 예정 운행이 아직 시작되지 않았습니다.",

            timeLabel = "오전 9:08",

            category = AlarmCategory.Operation,

            isUnread = true,

        ),

        OperationAlarm(

            id = "alarm-3",

            title = "배정 변경",

            body = "10:00 운행 차량이 1호차에서 2호차로 변경되었습니다.",

            timeLabel = "오전 8:20",

            category = AlarmCategory.AssignmentChange,

            isUnread = false,

        ),

        OperationAlarm(

            id = "alarm-4",

            title = "출발시간 변경",

            body = "기흥역 통학버스 출발 시간이 09:05에서 09:25로 변경되었습니다.",

            timeLabel = "오전 8:45",

            category = AlarmCategory.DepartureTimeChange,

            isUnread = true,

        ),

        OperationAlarm(

            id = "alarm-5",

            title = "운행 취소",

            body = "기흥역 통학버스 09:05 운행이 기상 악화(폭우 경보)로 취소되었습니다.",

            timeLabel = "오전 8:30",

            category = AlarmCategory.OperationCancel,

            isUnread = true,

        ),

        OperationAlarm(

            id = "alarm-6",

            title = "중단 요청 처리 결과",

            body = "중단 요청이 확인 되었으며, 관리자 안내가 등록되었습니다.",

            timeLabel = "어제",

            category = AlarmCategory.Operation,

            isUnread = false,

        ),

    )



    private val driver02SeedItems: List<OperationAlarm> = listOf(

        OperationAlarm(

            id = "alarm-1",

            title = "출발시간 임박",

            body = "수원역 통학버스 1호차 운행이 10분 후 시작됩니다.",

            timeLabel = "오전 8:30",

            category = AlarmCategory.Operation,

            isUnread = true,

        ),

        OperationAlarm(

            id = "alarm-2",

            title = "운행 미시작 확인",

            body = "08:40 예정 운행이 아직 시작되지 않았습니다.",

            timeLabel = "오전 8:43",

            category = AlarmCategory.Operation,

            isUnread = true,

        ),

        OperationAlarm(

            id = "alarm-3",

            title = "배정 변경",

            body = "11:10 운행 차량이 4호차에서 1호차로 변경되었습니다.",

            timeLabel = "오전 7:50",

            category = AlarmCategory.AssignmentChange,

            isUnread = false,

        ),

        OperationAlarm(

            id = "alarm-4",

            title = "출발시간 변경",

            body = "영통역 셔틀 출발 시간이 11:10에서 11:25로 변경되었습니다.",

            timeLabel = "오전 8:05",

            category = AlarmCategory.DepartureTimeChange,

            isUnread = true,

        ),

        OperationAlarm(

            id = "alarm-5",

            title = "운행 취소",

            body = "보정·기흥 순환 14:20 운행이 도로 통제로 취소되었습니다.",

            timeLabel = "오전 7:40",

            category = AlarmCategory.OperationCancel,

            isUnread = true,

        ),

        OperationAlarm(

            id = "alarm-6",

            title = "중단 요청 처리 결과",

            body = "중단 요청이 확인 되었으며, 관리자 안내가 등록되었습니다.",

            timeLabel = "어제",

            category = AlarmCategory.Operation,

            isUnread = false,

        ),

    )



    /** 시드 원본 (isUnread = 최초 미확인 여부). 로그인 계정별. */

    val seedItems: List<OperationAlarm>

        get() = when (SessionStateHolder.currentUserId) {

            "user02" -> driver02SeedItems

            else -> driver01SeedItems

        }



    val items: List<OperationAlarm>

        get() = withReadState(seedItems)



    fun withReadState(source: List<OperationAlarm> = seedItems): List<OperationAlarm> =

        source.map { it.copy(isUnread = AlarmReadStateHolder.isUnread(it.id)) }



    fun filtered(filter: AlarmFilter): List<OperationAlarm> {

        val current = items

        return when (filter) {

            AlarmFilter.All -> current

            AlarmFilter.Unread -> current.filter { it.isUnread }

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


