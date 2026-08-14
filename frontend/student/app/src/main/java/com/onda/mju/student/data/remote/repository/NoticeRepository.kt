package com.onda.mju.student.data.remote.repository

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Place
import com.onda.mju.student.data.remote.SupabaseClientProvider
import com.onda.mju.student.data.remote.dto.NoticeDto
import com.onda.mju.student.ui.screen.notice.NoticeAttachment
import com.onda.mju.student.ui.screen.notice.NoticeBadge
import com.onda.mju.student.ui.screen.notice.NoticeItem
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class NoticeRepository {
    private val zone = ZoneId.of("Asia/Seoul")
    private val dateTimeFmt = DateTimeFormatter.ofPattern("yyyy.MM.dd / HH:mm")

    /**
     * 학생 대상·게시 기간 내 공지.
     * RLS가 STUDENT audience 를 걸어도, 클라이언트에서 한 번 더 검사한다.
     */
    suspend fun fetchPublishedForStudent(): Result<List<NoticeItem>> = runCatching {
        val rows = SupabaseClientProvider.client
            .from("notices")
            .select(
                columns = Columns.raw(
                    "id,title,content,type,audience,status,created_at,updated_at,starts_at,ends_at",
                ),
            )
            .decodeList<NoticeDto>()
        val now = Instant.now()
        rows
            .filter { audienceIncludesStudent(it.audience) }
            .filter { it.status.equals("PUBLISHED", true) || it.status.equals("SCHEDULED", true) }
            .filter { inPublishWindow(it.startsAt, it.endsAt, now) }
            .sortedByDescending { it.startsAt ?: it.createdAt }
            .map { it.toNoticeItem() }
    }

    private fun audienceIncludesStudent(raw: JsonElement?): Boolean {
        val tokens = parseAudience(raw)
        // RLS가 이미 학생만 내려주면 audience 파싱 실패 시에도 목록에 남긴다.
        if (tokens.isEmpty()) return true
        return tokens.any { it.equals("STUDENT", ignoreCase = true) }
    }

    private fun parseAudience(raw: JsonElement?): List<String> {
        if (raw == null) return emptyList()
        return when (raw) {
            is JsonArray -> raw.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            is JsonPrimitive -> raw.contentOrNull
                ?.replace("{", "")
                ?.replace("}", "")
                ?.split(',', '|', '/')
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                .orEmpty()
            else -> emptyList()
        }
    }

    private fun inPublishWindow(startsAt: String?, endsAt: String?, now: Instant): Boolean {
        val startOk = startsAt.isNullOrBlank() || runCatching {
            OffsetDateTime.parse(startsAt).toInstant() <= now
        }.getOrDefault(true)
        val endOk = endsAt.isNullOrBlank() || runCatching {
            OffsetDateTime.parse(endsAt).toInstant() >= now
        }.getOrDefault(true)
        return startOk && endOk
    }

    private fun NoticeDto.toNoticeItem(): NoticeItem {
        val typeKey = type.orEmpty().uppercase()
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
        val body = stripAttachments(content.trim())
        val plain = stripHtml(body)
        val description = plain.lineSequence().firstOrNull { it.isNotBlank() }
        return NoticeItem(
            id = id,
            title = title.replace(Regex("^\\[.*?\\]\\s*"), "").ifBlank { title },
            description = description?.takeIf { it != title },
            datetime = formatDateTime(startsAt ?: createdAt),
            badge = badge,
            icon = if (typeKey == "GENERAL") Icons.Filled.Place else icon,
            isUrgentCard = typeKey == "URGENT",
            edited = isEdited(createdAt, updatedAt),
            body = body.ifBlank { title },
            attachments = parseAttachments(content),
        )
    }

    private val attachBlockRegex = Regex(
        """<div[^>]*onda-notice-attach[^>]*>\s*<a[^>]*href=["']([^"']+)["'][^>]*>(.*?)</a>\s*</div>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    private fun parseAttachments(html: String): List<NoticeAttachment> {
        if (html.isBlank()) return emptyList()
        val seen = linkedSetOf<String>()
        val out = mutableListOf<NoticeAttachment>()
        fun add(url: String, name: String) {
            val href = url.trim()
            if (href.isBlank() || !seen.add(href)) return
            val label = stripHtml(name).ifBlank { href.substringAfterLast('/') }
            out += NoticeAttachment(name = label, url = href)
        }
        attachBlockRegex.findAll(html).forEach { add(it.groupValues[1], it.groupValues[2]) }
        return out
    }

    private fun stripAttachments(html: String): String =
        attachBlockRegex.replace(html, "").trim()

    suspend fun incrementView(id: String) {
        runCatching {
            SupabaseClientProvider.client.postgrest.rpc("increment_notice_view") {
                parameter("p_notice_id", id)
            }
        }
    }

    private fun stripHtml(raw: String): String {
        if (raw.isBlank()) return ""
        return raw
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun isEdited(createdAt: String?, updatedAt: String?): Boolean {
        val created = parseInstant(createdAt) ?: return false
        val updated = parseInstant(updatedAt) ?: return false
        return Duration.between(created, updated).seconds >= 2
    }

    private fun formatDateTime(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        return runCatching {
            OffsetDateTime.parse(iso).atZoneSameInstant(zone).toLocalDateTime().format(dateTimeFmt)
        }.getOrDefault(iso)
    }

    private fun parseInstant(iso: String?): Instant? {
        if (iso.isNullOrBlank()) return null
        return runCatching { OffsetDateTime.parse(iso).toInstant() }.getOrNull()
    }
}
