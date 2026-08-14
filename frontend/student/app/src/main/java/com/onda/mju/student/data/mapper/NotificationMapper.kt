package com.onda.mju.student.data.mapper

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.graphics.Color
import com.onda.mju.student.data.notification.NoticeAlertReadStore
import com.onda.mju.student.data.remote.dto.NotificationDto
import com.onda.mju.student.ui.screen.notice.NoticeItem
import com.onda.mju.student.ui.screen.notification.NotificationDetail
import com.onda.mju.student.ui.screen.notification.NotificationFilter
import com.onda.mju.student.ui.screen.notification.NotificationItem
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val seoulZone: ZoneId = ZoneId.of("Asia/Seoul")
private val displayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm").withZone(seoulZone)

private val EmergencyRed = Color(0xFFE11D48)
private val OperationBlue = Color(0xFF2563EB)
private val NoticeOrange = Color(0xFFF59E0B)
private val ServiceGray = Color(0xFF6B7280)

private val EmergencyIconBg = Color(0xFFFEE2E6)
private val OperationIconBg = Color(0xFFE8F1FE)
private val NoticeIconBg = Color(0xFFFFF1D6)
private val ServiceIconBg = Color(0xFFF1F3F5)

private val EmergencyIconTint = Color(0xFFE45A73)
private val OperationIconTint = Color(0xFF5B8DEF)
private val NoticeIconTint = Color(0xFFF0A93B)
private val ServiceIconTint = Color(0xFF8B939E)

fun NoticeItem.toNotificationItem(readNoticeAlertIds: Set<String>): NotificationItem {
    val alertId = NoticeAlertReadStore.alertIdForNotice(id)
    val typeKey = type.uppercase()
    val (categoryLabel, categoryColor, iconBg, iconTint, icon, filters) = when (typeKey) {
        "URGENT" -> AlertStyle(
            "긴급 공지",
            EmergencyRed,
            EmergencyIconBg,
            EmergencyIconTint,
            Icons.Filled.Notifications,
            setOf(NotificationFilter.Important, NotificationFilter.Operation),
        )
        "IMPORTANT" -> AlertStyle(
            "중요 공지",
            EmergencyRed,
            EmergencyIconBg,
            EmergencyIconTint,
            Icons.Filled.Campaign,
            setOf(NotificationFilter.Important),
        )
        "OPERATION_CHANGE" -> AlertStyle(
            "운행 변경",
            OperationBlue,
            OperationIconBg,
            OperationIconTint,
            Icons.Filled.DirectionsBus,
            setOf(NotificationFilter.Operation),
        )
        else -> AlertStyle(
            "공지",
            NoticeOrange,
            NoticeIconBg,
            NoticeIconTint,
            Icons.Filled.Campaign,
            setOf(NotificationFilter.Operation),
        )
    }
    val bodyText = body.trim().ifBlank { title }
    val attachHint = resolvedAttachments
        .takeIf { it.isNotEmpty() }
        ?.joinToString(prefix = "\n\n첨부파일: ") { it.name }
        .orEmpty()
    val detailBody = (bodyText + attachHint).trim()
    val subtitle = description?.takeIf { it.isNotBlank() && it != title && !it.contains('<') }
    val detailDatetime = datetime.replace(" / ", " ")
    return NotificationItem(
        id = alertId,
        categoryLabel = categoryLabel,
        categoryColor = categoryColor,
        iconBg = iconBg,
        iconTint = iconTint,
        icon = icon,
        title = title,
        subtitle = subtitle,
        timeLabel = relativeTimeLabel(createdAtIso, datetime),
        filters = filters,
        initiallyUnread = alertId !in readNoticeAlertIds,
        sortInstantMillis = parseSortMillis(createdAtIso, datetime),
        detail = NotificationDetail(
            categoryLabel = categoryLabel,
            categoryColor = categoryColor,
            title = title,
            datetime = detailDatetime,
            body = detailBody,
            infoBanner = null,
            relatedMenuTitle = "공지사항에서 보기",
        ),
    )
}

fun NotificationDto.toNotificationItem(): NotificationItem {
    val typeKey = type.uppercase()
    val (categoryLabel, categoryColor, iconBg, iconTint, icon, filters) = when (typeKey) {
        "OPERATION" -> AlertStyle(
            "운행 알림",
            OperationBlue,
            OperationIconBg,
            OperationIconTint,
            Icons.Filled.DirectionsBus,
            setOf(NotificationFilter.Operation),
        )
        "SYSTEM" -> AlertStyle(
            "서비스",
            ServiceGray,
            ServiceIconBg,
            ServiceIconTint,
            Icons.Filled.Info,
            setOf(NotificationFilter.Service),
        )
        else -> AlertStyle(
            "공지",
            NoticeOrange,
            NoticeIconBg,
            NoticeIconTint,
            Icons.Filled.Campaign,
            setOf(NotificationFilter.Important, NotificationFilter.Operation),
        )
    }
    val bodyText = message.trim().ifBlank { title }
    val subtitle = bodyText.take(80).takeIf { it != title.trim() }
    val detailDatetime = formatAbsolute(createdAt)
    return NotificationItem(
        id = id,
        categoryLabel = categoryLabel,
        categoryColor = categoryColor,
        iconBg = iconBg,
        iconTint = iconTint,
        icon = icon,
        title = title.trim().ifBlank { "알림" },
        subtitle = subtitle,
        timeLabel = relativeTimeLabel(createdAt, detailDatetime),
        filters = filters,
        initiallyUnread = !(isRead ?: false),
        sortInstantMillis = parseSortMillis(createdAt, detailDatetime),
        detail = NotificationDetail(
            categoryLabel = categoryLabel,
            categoryColor = categoryColor,
            title = title.trim().ifBlank { "알림" },
            datetime = detailDatetime,
            body = bodyText,
            relatedMenuTitle = null,
        ),
    )
}

private data class AlertStyle(
    val categoryLabel: String,
    val categoryColor: Color,
    val iconBg: Color,
    val iconTint: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val filters: Set<NotificationFilter>,
)

private fun relativeTimeLabel(startsAtOrCreated: String?, fallbackDatetime: String): String {
    val instant = parseInstant(startsAtOrCreated) ?: return fallbackDatetime
    val minutes = Duration.between(instant, Instant.now()).toMinutes()
    return when {
        minutes < 1 -> "방금 전"
        minutes < 60 -> "${minutes}분 전"
        minutes < 60 * 24 -> "${minutes / 60}시간 전"
        else -> displayFormatter.format(instant)
    }
}

private fun formatAbsolute(raw: String?): String {
    val instant = parseInstant(raw) ?: return raw?.takeIf { it.isNotBlank() } ?: "-"
    return displayFormatter.format(instant)
}

private fun parseInstant(raw: String?): Instant? {
    if (raw.isNullOrBlank()) return null
    return runCatching { Instant.parse(raw) }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(raw).toInstant() }.getOrNull()
}

private fun parseSortMillis(iso: String?, displayDatetime: String): Long {
    parseInstant(iso)?.toEpochMilli()?.let { return it }
    // yyyy.MM.dd HH:mm / yyyy.MM.dd / HH:mm
    val normalized = displayDatetime.replace(" / ", " ").trim()
    return runCatching {
        java.time.LocalDateTime
            .parse(normalized, DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"))
            .atZone(seoulZone)
            .toInstant()
            .toEpochMilli()
    }.getOrDefault(0L)
}
