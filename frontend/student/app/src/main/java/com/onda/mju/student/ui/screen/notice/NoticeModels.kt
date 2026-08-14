package com.onda.mju.student.ui.screen.notice

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Place
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class NoticeBadge(val label: String, val color: Color) {
    Important("중요 공지", Color(0xFFE11D48)),
    ImportantShort("중요", Color(0xFFE11D48)),
    Operation("운행 변경", Color(0xFF2563EB)),
    General("일반", Color(0xFF60A5FA)),
}

data class NoticeAttachment(
    val name: String,
    val url: String,
)

data class NoticeItem(
    val id: String,
    val title: String,
    val description: String?,
    val datetime: String,
    val badge: NoticeBadge,
    val icon: ImageVector,
    val isUrgentCard: Boolean = false,
    val edited: Boolean = false,
    val body: String,
    val relatedRoutes: List<String> = emptyList(),
    val attachments: List<NoticeAttachment> = emptyList(),
) {
    fun datetimeLabel(): String = if (edited) "$datetime · 수정됨" else datetime
}

fun sampleNotices(): List<NoticeItem> = listOf(
    NoticeItem(
        id = "1",
        title = "폭우로 인해 오늘 18시 이후 모든 셔틀 운행이 종료됩니다.",
        description = null,
        datetime = "2026.06.20 / 15:45",
        badge = NoticeBadge.Important,
        icon = Icons.Filled.Campaign,
        isUrgentCard = true,
        body = "안녕하세요, ON-DA 운영팀입니다.\n\n기상특보가 발효되어 학생 여러분의 안전을 위해 " +
            "오늘 18시 이후 모든 셔틀 운행을 종료합니다.\n\n" +
            "• 기흥역 통학버스\n• 명지대역 통학버스\n• 시내 셔틀\n\n" +
            "운행 재개 여부는 추가 공지로 안내드리겠습니다. 이용해 주셔서 감사합니다.",
        relatedRoutes = listOf("기흥역 통학버스", "명지대역 통학버스", "시내 셔틀"),
    ),
    NoticeItem(
        id = "2",
        title = "기흥역 통학버스 17:15 차량 5대 운행",
        description = "기존 4대에서 5대로 증차 운행합니다. 이용에 참고해 주세요.",
        datetime = "2026.06.20 / 14:20",
        badge = NoticeBadge.ImportantShort,
        icon = Icons.Filled.DirectionsBus,
        edited = true,
        body = "기흥역 통학버스 17:15 회차가 기존 4대에서 5대로 증차됩니다.",
        relatedRoutes = listOf("기흥역 통학버스"),
    ),
    NoticeItem(
        id = "3",
        title = "방학 중 셔틀버스 시간표 안내",
        description = "7/1(화)부터 방학 시간표가 적용됩니다.",
        datetime = "2026.06.19 / 11:00",
        badge = NoticeBadge.Operation,
        icon = Icons.Filled.Event,
        body = "여름방학 기간 동안 셔틀버스 운행 시간표가 변경됩니다. 7/1(화)부터 방학 시간표가 적용됩니다.",
        relatedRoutes = listOf("기흥역 통학버스", "명지대역 통학버스", "시내 셔틀"),
    ),
    NoticeItem(
        id = "4",
        title = "명지대역 사거리 정류장 위치 안내",
        description = "정류장 위치가 소폭 조정되었습니다.",
        datetime = "2026.06.18 / 09:30",
        badge = NoticeBadge.General,
        icon = Icons.Filled.Place,
        body = "명지대역 사거리 정류장 대기 위치가 횡단보도 쪽으로 소폭 이동했습니다.",
        relatedRoutes = listOf("명지대역 통학버스"),
    ),
)

fun List<NoticeItem>.search(query: String): List<NoticeItem> {
    val q = query.trim()
    if (q.isEmpty()) return this
    return filter { it.title.contains(q, ignoreCase = true) }
}
