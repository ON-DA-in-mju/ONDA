package com.onda.mju.student.data.mapper

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Event
import com.onda.mju.student.data.remote.dto.NoticeDto
import com.onda.mju.student.ui.screen.notice.NoticeBadge
import com.onda.mju.student.ui.screen.notice.NoticeContentParser
import com.onda.mju.student.ui.screen.notice.NoticeItem
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val seoulZone: ZoneId = ZoneId.of("Asia/Seoul")
private val displayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy.MM.dd / HH:mm").withZone(seoulZone)

fun NoticeDto.toNoticeItem(): NoticeItem {
    val typeKey = type.uppercase()
    val badge = when (typeKey) {
        "URGENT" -> NoticeBadge.Important
        "IMPORTANT" -> NoticeBadge.ImportantShort
        "OPERATION_CHANGE" -> NoticeBadge.Operation
        else -> NoticeBadge.General
    }
    val icon = when (typeKey) {
        "URGENT", "IMPORTANT" -> Icons.Filled.Campaign
        "OPERATION_CHANGE" -> Icons.Filled.DirectionsBus
        else -> Icons.Filled.Event
    }
    val parsed = NoticeContentParser.parse(content)
    val bodyText = parsed.body.ifBlank { title.trim() }
    val description = bodyText
        .lineSequence()
        .map { it.trim() }
        .firstOrNull { it.isNotEmpty() }
        ?.take(80)
    val edited = !updatedAt.isNullOrBlank() &&
        !createdAt.isNullOrBlank() &&
        updatedAt != createdAt
    val firstAttach = parsed.attachments.firstOrNull()
    return NoticeItem(
        id = id,
        title = title.trim().ifBlank { "공지" },
        description = description?.takeIf { it != title.trim() },
        datetime = formatNoticeDateTime(startsAt ?: createdAt),
        badge = badge,
        icon = icon,
        isUrgentCard = typeKey == "URGENT",
        edited = edited,
        body = bodyText.ifBlank { title },
        relatedRoutes = emptyList(),
        attachments = parsed.attachments,
        attachmentName = firstAttach?.name,
        attachmentMeta = firstAttach?.meta,
        attachmentUrl = firstAttach?.url,
        status = status,
        type = typeKey,
        createdAtIso = startsAt ?: createdAt,
    )
}

private fun formatNoticeDateTime(raw: String?): String {
    if (raw.isNullOrBlank()) return "-"
    val instant = runCatching { Instant.parse(raw) }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(raw).toInstant() }.getOrNull()
        ?: return raw
    return displayFormatter.format(instant)
}
