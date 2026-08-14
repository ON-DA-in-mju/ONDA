package com.onda.mju.student.data.mapper

import com.onda.mju.student.data.remote.dto.ReportCommentDto
import com.onda.mju.student.data.remote.dto.ReportCommentInsertDto
import com.onda.mju.student.data.remote.dto.ReportDto
import com.onda.mju.student.data.remote.dto.ReportInsertDto
import com.onda.mju.student.data.remote.dto.ReportUpdateDto
import com.onda.mju.student.ui.screen.community.CommunityComment
import com.onda.mju.student.ui.screen.community.CommunityPost
import com.onda.mju.student.ui.screen.community.ReportReaction
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val seoulZone: ZoneId = ZoneId.of("Asia/Seoul")
private val registeredFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm").withZone(seoulZone)

fun ReportDto.toCommunityPost(
    likeCount: Int = 0,
    dislikeCount: Int = 0,
    myReaction: ReportReaction? = null,
    commentCount: Int = 0,
): CommunityPost {
    val created = createdAt?.let { parseInstant(it) }
    return CommunityPost(
        id = id,
        userId = userId,
        title = title,
        body = content,
        timeLabel = relativeTimeLabel(created),
        registeredAt = created?.let { registeredFormatter.format(it) } ?: "-",
        likeCount = likeCount,
        dislikeCount = dislikeCount,
        myReaction = myReaction,
        commentCount = commentCount,
        viewCount = viewCount ?: 0,
    )
}

fun CommunityPost.toPostInsertDto(userId: String): ReportInsertDto =
    ReportInsertDto(
        userId = userId,
        title = title.trim(),
        content = body.trim(),
        status = "PENDING",
        source = "STUDENT",
        boardType = "POST",
        category = "POST",
    )

fun CommunityPost.toPostUpdateDto(updatedAtIso: String = Instant.now().toString()): ReportUpdateDto =
    ReportUpdateDto(
        title = title.trim(),
        content = body.trim(),
        category = "POST",
        updatedAt = updatedAtIso,
    )

fun ReportCommentDto.toCommunityComment(currentUserId: String?): CommunityComment {
    val created = createdAt?.let { parseInstant(it) }
    val deleted = isDeleted || content.trim() == "삭제된 댓글입니다."
    return CommunityComment(
        id = id,
        postId = reportId,
        userId = userId,
        body = content,
        timeLabel = relativeTimeLabel(created),
        isMine = currentUserId != null && currentUserId == userId,
        isDeleted = deleted,
    )
}

fun CommunityComment.toInsertDto(userId: String): ReportCommentInsertDto =
    ReportCommentInsertDto(
        reportId = postId,
        userId = userId,
        content = body.trim(),
    )

private fun parseInstant(raw: String): Instant? =
    runCatching { Instant.parse(raw) }.getOrNull()
        ?: runCatching { ZonedDateTime.parse(raw).toInstant() }.getOrNull()

private fun relativeTimeLabel(created: Instant?): String {
    if (created == null) return "-"
    val minutes = Duration.between(created, Instant.now()).toMinutes().coerceAtLeast(0)
    return when {
        minutes < 1 -> "방금 전"
        minutes < 60 -> "${minutes}분 전"
        minutes < 60 * 24 -> "${minutes / 60}시간 전"
        else -> "${minutes / (60 * 24)}일 전"
    }
}
