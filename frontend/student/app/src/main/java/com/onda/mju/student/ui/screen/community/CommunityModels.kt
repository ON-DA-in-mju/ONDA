package com.onda.mju.student.ui.screen.community

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.material.icons.filled.EventSeat

enum class CommunityFilter(val label: String) {
    All("전체"),
    Arrival("버스 도착"),
    Full("만석"),
    Queue("대기줄"),
    Traffic("교통 정체"),
}

enum class ReportReaction(val dbValue: String) {
    Like("LIKE"),
    Dislike("DISLIKE");

    companion object {
        fun fromDb(value: String?): ReportReaction? =
            entries.firstOrNull { it.dbValue.equals(value, ignoreCase = true) }
    }
}

enum class ReportType(
    val label: String,
    val color: Color,
    val icon: ImageVector,
) {
    Full("만석", Color(0xFF10B981), Icons.Filled.Groups),
    Other("기타", Color(0xFFF59E0B), Icons.Filled.MoreHoriz),
    LongQueue("대기줄 김", Color(0xFFEF4444), Icons.Filled.Groups),
    TrafficJam("교통 정체", Color(0xFF3B82F6), Icons.Filled.Traffic),
    Arrival("버스 출발/도착", Color(0xFF0041F1), Icons.Filled.DirectionsBus),
    SeatAvailable("좌석 여유", Color(0xFF14B8A6), Icons.Filled.EventSeat),
    ShortQueue("대기줄 짧음", Color(0xFF22C55E), Icons.Filled.Groups),
    Passed("버스가 지나감", Color(0xFF6366F1), Icons.Filled.DirectionsBus),
}

data class CommunityReport(
    val id: String,
    val type: ReportType,
    val routeLabel: String,
    val directionLabel: String,
    val stopName: String,
    val timeLabel: String,
    val reporterCount: Int,
    val likeCount: Int,
    val dislikeCount: Int,
    val body: String,
    val filters: Set<CommunityFilter>,
    val vehicleLabel: String = "2호차",
    val registeredAt: String = "2026.06.20 16:42",
    val isValid: Boolean = true,
    /** public.reports.user_id — DB 연동 시 본인 제보 구분용 */
    val userId: String = "",
    /** public.reports.status: PENDING | PROCESSING | COMPLETED */
    val status: String = "PENDING",
    /** 현재 로그인 사용자의 반응 (없으면 null) */
    val myReaction: ReportReaction? = null,
    val commentCount: Int = 0,
    /** 고유 조회수 (몇 명이 봤는지) */
    val viewCount: Int = 0,
)

/** 좋아요/싫어요 토글 후 카운트·내 반응을 로컬에 반영 */
fun CommunityReport.withToggledReaction(target: ReportReaction): CommunityReport {
    val next = if (myReaction == target) null else target
    var likes = likeCount
    var dislikes = dislikeCount
    when (myReaction) {
        ReportReaction.Like -> likes = (likes - 1).coerceAtLeast(0)
        ReportReaction.Dislike -> dislikes = (dislikes - 1).coerceAtLeast(0)
        null -> Unit
    }
    when (next) {
        ReportReaction.Like -> likes += 1
        ReportReaction.Dislike -> dislikes += 1
        null -> Unit
    }
    return copy(likeCount = likes, dislikeCount = dislikes, myReaction = next)
}

fun sampleCommunityReports(): List<CommunityReport> = listOf(
    CommunityReport(
        id = "r1",
        type = ReportType.Full,
        routeLabel = "기흥역 통학버스",
        directionLabel = "명지대 방면",
        stopName = "명지대역 사거리 정류장",
        timeLabel = "2분 전",
        reporterCount = 5,
        likeCount = 5,
        dislikeCount = 1,
        body = "퇴근 시간대라 좌석이 거의 가득 찼고, 버스 내부에 서 있는 승객이 많았어요.",
        filters = setOf(CommunityFilter.Full),
        vehicleLabel = "2호차",
        registeredAt = "2026.06.20 16:42",
    ),
    CommunityReport(
        id = "r2",
        type = ReportType.Other,
        routeLabel = "명지대역 통학버스",
        directionLabel = "명지대 방면",
        stopName = "명진당",
        timeLabel = "5분 전",
        reporterCount = 3,
        likeCount = 3,
        dislikeCount = 0,
        body = "정류장 앞 공사가 있어 대기 위치가 조금 이동했어요.",
        filters = setOf(CommunityFilter.All),
    ),
    CommunityReport(
        id = "r3",
        type = ReportType.LongQueue,
        routeLabel = "시내 셔틀",
        directionLabel = "시내 방면",
        stopName = "버스관리사무소",
        timeLabel = "8분 전",
        reporterCount = 6,
        likeCount = 6,
        dislikeCount = 2,
        body = "대기줄이 길어서 다음 회차를 노리는 편이 나을 수 있어요.",
        filters = setOf(CommunityFilter.Queue),
    ),
    CommunityReport(
        id = "r4",
        type = ReportType.TrafficJam,
        routeLabel = "기흥역 통학버스",
        directionLabel = "기흥역 방면",
        stopName = "기흥역 5번 출구",
        timeLabel = "12분 전",
        reporterCount = 2,
        likeCount = 2,
        dislikeCount = 0,
        body = "기흥역 방면 도로가 정체되어 도착이 지연되고 있어요.",
        filters = setOf(CommunityFilter.Traffic),
    ),
)

fun List<CommunityReport>.filterBy(filter: CommunityFilter): List<CommunityReport> =
    if (filter == CommunityFilter.All) this
    else filter { filter in it.filters || it.type.label.contains(filter.label.take(2)) }

fun ReportType.toCommunityFilters(): Set<CommunityFilter> = when (this) {
    ReportType.Full -> setOf(CommunityFilter.Full)
    ReportType.LongQueue, ReportType.ShortQueue -> setOf(CommunityFilter.Queue)
    ReportType.TrafficJam -> setOf(CommunityFilter.Traffic)
    ReportType.Arrival, ReportType.Passed -> setOf(CommunityFilter.Arrival)
    ReportType.Other, ReportType.SeatAvailable -> emptySet()
}

fun createCommunityReport(
    type: ReportType,
    routeLabel: String,
    stopName: String,
    body: String,
    directionLabel: String = "명지대 방면",
    vehicleLabel: String = "2호차",
    id: String = "r_${System.currentTimeMillis()}",
    reporterCount: Int = 1,
    likeCount: Int = 0,
    dislikeCount: Int = 0,
    registeredAt: String = "방금 전",
    isValid: Boolean = true,
    userId: String = "",
    status: String = "PENDING",
): CommunityReport = CommunityReport(
    id = id,
    type = type,
    routeLabel = routeLabel,
    directionLabel = directionLabel,
    stopName = stopName,
    timeLabel = "방금 전",
    reporterCount = reporterCount,
    likeCount = likeCount,
    dislikeCount = dislikeCount,
    body = body.ifBlank { "${type.label} 상황을 제보했습니다." },
    filters = type.toCommunityFilters(),
    vehicleLabel = vehicleLabel,
    registeredAt = registeredAt,
    isValid = isValid,
    userId = userId,
    status = status,
)
