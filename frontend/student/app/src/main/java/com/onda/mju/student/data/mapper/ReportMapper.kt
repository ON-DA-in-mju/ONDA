package com.onda.mju.student.data.mapper

import com.onda.mju.student.data.remote.dto.ReportDto
import com.onda.mju.student.data.remote.dto.ReportInsertDto
import com.onda.mju.student.data.remote.dto.ReportUpdateDto
import com.onda.mju.student.ui.screen.community.CommunityReport
import com.onda.mju.student.ui.screen.community.ReportReaction
import com.onda.mju.student.ui.screen.community.ReportType
import com.onda.mju.student.ui.screen.community.toCommunityFilters
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val seoulZone: ZoneId = ZoneId.of("Asia/Seoul")
private val registeredFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm").withZone(seoulZone)

private val metaKeys = listOf("노선", "방향", "정류장", "차량", "유형")

fun buildReportTitle(type: ReportType, routeLabel: String, stopName: String): String =
    "[${type.label}] $routeLabel · $stopName"

fun buildReportContent(
    type: ReportType,
    routeLabel: String,
    directionLabel: String,
    stopName: String,
    vehicleLabel: String,
    body: String,
): String = buildString {
    appendLine("노선: $routeLabel")
    appendLine("방향: $directionLabel")
    appendLine("정류장: $stopName")
    appendLine("차량: $vehicleLabel")
    appendLine("유형: ${type.label}")
    appendLine()
    append(body.trim().ifBlank { "${type.label} 상황을 제보했습니다." })
}

fun CommunityReport.toInsertDto(userId: String): ReportInsertDto =
    ReportInsertDto(
        userId = userId,
        title = buildReportTitle(type, routeLabel, stopName),
        content = buildReportContent(type, routeLabel, directionLabel, stopName, vehicleLabel, body),
        status = status.ifBlank { "PENDING" },
        source = "STUDENT",
        boardType = "REPORT",
        category = type.name,
    )

fun CommunityReport.toUpdateDto(updatedAtIso: String = Instant.now().toString()): ReportUpdateDto =
    ReportUpdateDto(
        title = buildReportTitle(type, routeLabel, stopName),
        content = buildReportContent(type, routeLabel, directionLabel, stopName, vehicleLabel, body),
        category = type.name,
        updatedAt = updatedAtIso,
    )

fun ReportDto.toCommunityReport(
    likeCount: Int = 0,
    dislikeCount: Int = 0,
    myReaction: ReportReaction? = null,
    commentCount: Int = 0,
): CommunityReport {
    val meta = parseReportMeta(content)
    val type = resolveReportType(category, meta["유형"])
    val routeLabel = meta["노선"] ?: title.substringAfter("] ", title).substringBefore(" ·").ifBlank { "노선 미상" }
    val stopName = meta["정류장"] ?: title.substringAfter(" · ", "").ifBlank { "정류장 미상" }
    val directionLabel = meta["방향"] ?: "방면 미상"
    val vehicleLabel = meta["차량"] ?: "-"
    val body = stripReportMeta(content).ifBlank { content }
    val created = createdAt?.let { parseInstant(it) }
    return CommunityReport(
        id = id,
        type = type,
        routeLabel = routeLabel,
        directionLabel = directionLabel,
        stopName = stopName,
        timeLabel = relativeTimeLabel(created),
        reporterCount = 1,
        likeCount = likeCount,
        dislikeCount = dislikeCount,
        body = body,
        filters = type.toCommunityFilters(),
        vehicleLabel = vehicleLabel,
        registeredAt = created?.let { registeredFormatter.format(it) } ?: "-",
        isValid = status != "COMPLETED",
        userId = userId,
        status = status,
        myReaction = myReaction,
        commentCount = commentCount,
        viewCount = viewCount ?: 0,
    )
}

private fun resolveReportType(category: String?, typeLabel: String?): ReportType {
    val byCategory = category?.let { runCatching { ReportType.valueOf(it) }.getOrNull() }
    if (byCategory != null) return byCategory
    return ReportType.entries.firstOrNull { it.label == typeLabel } ?: ReportType.Other
}

private fun parseReportMeta(content: String): Map<String, String> {
    val map = linkedMapOf<String, String>()
    content.lineSequence().forEach { line ->
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return@forEach
        val idx = trimmed.indexOf(':')
        if (idx <= 0) return@forEach
        val key = trimmed.substring(0, idx).trim()
        if (key in metaKeys) {
            map[key] = trimmed.substring(idx + 1).trim()
        }
    }
    return map
}

private fun stripReportMeta(content: String): String {
    val lines = content.lines()
    var i = 0
    while (i < lines.size) {
        val trimmed = lines[i].trim()
        if (trimmed.isEmpty()) {
            i++
            break
        }
        val key = trimmed.substringBefore(':', missingDelimiterValue = "").trim()
        if (key in metaKeys) {
            i++
            continue
        }
        break
    }
    while (i < lines.size && lines[i].trim().isEmpty()) i++
    return lines.drop(i).joinToString("\n").trim()
}

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
