package com.onda.mju.student.ui.screen.notification

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow

enum class NotificationFilter(val label: String) {
    All("전체"),
    Important("중요 공지"),
    Operation("운행 알림"),
    Alight("하차 알림"),
    Service("서비스"),
}

data class NotificationItem(
    val id: String,
    val categoryLabel: String,
    val categoryColor: Color,
    val iconBg: Color,
    val iconTint: Color,
    val icon: ImageVector,
    val title: String,
    val subtitle: String?,
    val timeLabel: String,
    val filters: Set<NotificationFilter>,
    val initiallyUnread: Boolean,
    val detail: NotificationDetail,
    val sortInstantMillis: Long = 0L,
)

data class NotificationDetail(
    val categoryLabel: String,
    val categoryColor: Color,
    val title: String,
    val datetime: String,
    val body: String,
    val comparison: ComparisonBlock? = null,
    val infoRows: List<DetailInfoRow> = emptyList(),
    val routeTag: String? = null,
    val infoBanner: String? = null,
    val relatedMenuTitle: String? = null,
)

data class ComparisonBlock(
    val beforeLabel: String,
    val beforeValue: String,
    val afterLabel: String,
    val afterValue: String,
)

data class DetailInfoRow(
    val label: String,
    val value: String,
    val isRouteTag: Boolean = false,
)

private val EmergencyRed = Color(0xFFE11D48)
private val OperationBlue = Color(0xFF2563EB)
private val AlightTeal = Color(0xFF14B8A6)
private val StartPurple = Color(0xFF8B5CF6)
private val NoticeOrange = Color(0xFFF59E0B)
private val ServiceGray = Color(0xFF6B7280)

/** Soft pastel circle backgrounds for list icons. */
private val EmergencyIconBg = Color(0xFFFEE2E6)
private val OperationIconBg = Color(0xFFE8F1FE)
private val AlightIconBg = Color(0xFFD8F5F0)
private val StartIconBg = Color(0xFFEFEAFE)
private val NoticeIconBg = Color(0xFFFFF1D6)
private val ServiceIconBg = Color(0xFFF1F3F5)

/** Same-family icon tints — softer than category labels, darker than iconBg. */
private val EmergencyIconTint = Color(0xFFE45A73)
private val OperationIconTint = Color(0xFF5B8DEF)
private val AlightIconTint = Color(0xFF3DBBA8)
private val StartIconTint = Color(0xFF9B7CED)
private val NoticeIconTint = Color(0xFFF0A93B)
private val ServiceIconTint = Color(0xFF8B939E)

/** Official notices suitable for the home "공식" banner. */
private val HomeBannerCategories = setOf("긴급 공지", "운행 변경", "공지")

/** Latest official notice for the home banner.
 * Sorted by detail.datetime (yyyy.MM.dd HH:mm) descending.
 */
fun List<NotificationItem>.latestHomeNotice(): NotificationItem? =
    filter { it.categoryLabel in HomeBannerCategories }
        .maxByOrNull { it.detail.datetime }

/** Unread count from the shared unread-id set. */
fun unreadNotificationCount(unreadIds: Set<String>): Int = unreadIds.size

/** Badge label for the home bell; empty when there are no unread alerts. */
fun formatUnreadBadgeLabel(unreadCount: Int): String = when {
    unreadCount <= 0 -> ""
    unreadCount > 99 -> "99+"
    else -> unreadCount.toString()
}

fun sampleNotifications(): List<NotificationItem> = listOf(
    NotificationItem(
        id = "sample-1",
        categoryLabel = "긴급 공지",
        categoryColor = EmergencyRed,
        iconBg = EmergencyIconBg,
        iconTint = EmergencyIconTint,
        icon = Icons.Filled.Notifications,
        title = "폭설로 인한 15시 이후 운행 중단 안내",
        subtitle = null,
        timeLabel = "2분 전",
        filters = setOf(NotificationFilter.Important, NotificationFilter.Operation),
        initiallyUnread = true,
        sortInstantMillis = 1,
        detail = NotificationDetail(
            categoryLabel = "긴급 공지",
            categoryColor = EmergencyRed,
            title = "폭설로 인한 15시 이후 운행 중단 안내",
            datetime = "2026.06.20 16:48",
            body = "폭설 영향으로 금일 15시 이후 모든 셔틀버스 운행이 일시 중단됩니다. 안전한 이동을 위해 대체 교통수단을 이용해 주세요.",
            infoRows = listOf(
                DetailInfoRow("적용 시간", "오늘 15:00 이후"),
                DetailInfoRow("대상 운행", "전체 노선"),
            ),
            infoBanner = "기상 상황에 따라 운행 재개 시각이 변동될 수 있습니다.",
            relatedMenuTitle = "전체 노선 운행 현황 보기",
        ),
    ),
    NotificationItem(
        id = "sample-2",
        categoryLabel = "운행 변경",
        categoryColor = OperationBlue,
        iconBg = OperationIconBg,
        iconTint = OperationIconTint,
        icon = Icons.Filled.DirectionsBus,
        title = "기흥역 통학버스 17:15 증차 안내",
        subtitle = "기존 3대에서 4대로 변경되었습니다.",
        timeLabel = "10분 전",
        filters = setOf(NotificationFilter.Operation),
        initiallyUnread = true,
        sortInstantMillis = 2,
        detail = NotificationDetail(
            categoryLabel = "운행 변경",
            categoryColor = OperationBlue,
            title = "기흥역 통학버스 17:15 증차 안내",
            datetime = "2026.06.20 14:20",
            body = "기흥역 통학버스 17:15 회차의 차량이 기존 3대에서 4대로 변경되었습니다.",
            comparison = ComparisonBlock(
                beforeLabel = "변경 전",
                beforeValue = "3대",
                afterLabel = "변경 후",
                afterValue = "4대",
            ),
            infoRows = listOf(
                DetailInfoRow("적용 일자", "2026.06.20(금)부터"),
                DetailInfoRow("대상 노선", "기흥역 통학버스", isRouteTag = true),
            ),
            routeTag = "기흥역 통학버스",
            infoBanner = "교통 상황에 따라 실제 운행 차량 수는 변동될 수 있습니다.",
            relatedMenuTitle = "기흥역 통학버스 시간표 보기",
        ),
    ),
    NotificationItem(
        id = "sample-3",
        categoryLabel = "하차 알림",
        categoryColor = AlightTeal,
        iconBg = AlightIconBg,
        iconTint = AlightIconTint,
        icon = Icons.Filled.Place,
        title = "기흥역 통학버스 2호차",
        subtitle = "하차 정류장 한 정류장 전입니다. 명지대입구 정류장에서 하차를 준비하세요.",
        timeLabel = "3분 전",
        filters = setOf(NotificationFilter.Alight),
        initiallyUnread = true,
        sortInstantMillis = 3,
        detail = NotificationDetail(
            categoryLabel = "하차 알림",
            categoryColor = AlightTeal,
            title = "기흥역 통학버스 2호차 하차 준비 안내",
            datetime = "2026.06.20 16:57",
            body = "하차 정류장 한 정류장 전입니다. 명지대입구 정류장에서 하차를 준비해 주세요.",
            infoRows = listOf(
                DetailInfoRow("하차 정류장", "명지대입구"),
                DetailInfoRow("대상 차량", "기흥역 통학버스 2호차"),
            ),
            infoBanner = "하차 시 안전에 유의해 주시고, 문이 열린 후 차례로 내려주세요.",
            relatedMenuTitle = "정류장 안내 보기",
        ),
    ),
    NotificationItem(
        id = "sample-4",
        categoryLabel = "운행 시작",
        categoryColor = StartPurple,
        iconBg = StartIconBg,
        iconTint = StartIconTint,
        icon = Icons.Filled.PlayArrow,
        title = "명지대역 셔틀 2호차 운행 시작",
        subtitle = "예정 16:50 · 실제 16:53",
        timeLabel = "15분 전",
        filters = setOf(NotificationFilter.Operation),
        initiallyUnread = false,
        sortInstantMillis = 4,
        detail = NotificationDetail(
            categoryLabel = "운행 시작",
            categoryColor = StartPurple,
            title = "명지대역 셔틀 2호차 운행 시작",
            datetime = "2026.06.20 16:53",
            body = "명지대역 셔틀 2호차가 운행을 시작했습니다.",
            infoRows = listOf(
                DetailInfoRow("예정 출발", "16:50"),
                DetailInfoRow("실제 출발", "16:53"),
                DetailInfoRow("대상 노선", "명지대역 통학버스", isRouteTag = true),
            ),
            routeTag = "명지대역 통학버스",
            infoBanner = "실시간 위치는 노선 화면에서 확인할 수 있습니다.",
            relatedMenuTitle = "명지대역 통학버스 위치 보기",
        ),
    ),
    NotificationItem(
        id = "sample-5",
        categoryLabel = "공지",
        categoryColor = NoticeOrange,
        iconBg = NoticeIconBg,
        iconTint = NoticeIconTint,
        icon = Icons.Filled.Campaign,
        title = "여름방학 중 운행 시간표 안내",
        subtitle = "7/1(화)부터 변경됩니다.",
        timeLabel = "1시간 전",
        filters = setOf(NotificationFilter.Operation),
        initiallyUnread = false,
        sortInstantMillis = 5,
        detail = NotificationDetail(
            categoryLabel = "공지",
            categoryColor = NoticeOrange,
            title = "여름방학 중 운행 시간표 안내",
            datetime = "2026.06.20 15:40",
            body = "여름방학 기간 동안 셔틀버스 운행 시간표가 변경됩니다. 7/1(화)부터 방학 시간표가 적용됩니다.",
            infoRows = listOf(
                DetailInfoRow("적용 일자", "2026.07.01(화)부터"),
                DetailInfoRow("대상 노선", "전체 노선"),
            ),
            infoBanner = "방학 중에는 일부 회차가 축소 운행될 수 있습니다.",
            relatedMenuTitle = "방학 시간표 확인하기",
        ),
    ),
    NotificationItem(
        id = "sample-6",
        categoryLabel = "서비스",
        categoryColor = ServiceGray,
        iconBg = ServiceIconBg,
        iconTint = ServiceIconTint,
        icon = Icons.Filled.Info,
        title = "ON-DA 앱이 최신 버전으로 업데이트되었습니다.",
        subtitle = null,
        timeLabel = "3시간 전",
        filters = setOf(NotificationFilter.Service),
        initiallyUnread = false,
        sortInstantMillis = 6,
        detail = NotificationDetail(
            categoryLabel = "서비스",
            categoryColor = ServiceGray,
            title = "ON-DA 앱이 최신 버전으로 업데이트되었습니다.",
            datetime = "2026.06.20 13:40",
            body = "앱 안정성과 알림 수신 성능이 개선되었습니다. 최신 버전을 이용해 주세요.",
            infoRows = listOf(
                DetailInfoRow("업데이트 버전", "1.0.1"),
                DetailInfoRow("주요 개선", "알림 안정성, 홈 화면 성능"),
            ),
            infoBanner = "업데이트 후에도 문제가 있으면 간편 제보로 알려주세요.",
            relatedMenuTitle = "간편 제보하기",
        ),
    ),
)
