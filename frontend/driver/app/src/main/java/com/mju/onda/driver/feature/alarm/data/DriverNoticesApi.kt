package com.mju.onda.driver.feature.alarm.data

import android.util.Log
import com.mju.onda.driver.core.supabase.SupabaseClient
import org.json.JSONArray
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 관리자 공지(notices) 중 기사 대상·게시 기간 내 항목을 조회한다.
 * audience 는 REST 필터 + 클라이언트에서 한 번 더 검사하고, 게시 기간도 재검사한다.
 */
object DriverNoticesApi {
    private const val TAG = "DriverNoticesApi"
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
    private val dateTimeFmt = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
    private val zone = ZoneId.systemDefault()

    data class DriverNotice(
        val id: String,
        val title: String,
        val content: String,
        val type: String,
        val createdAt: String?,
        val startsAt: String?,
        val endsAt: String?,
    )

    fun fetchPublishedForDriver(limit: Int = 30): List<DriverNotice> {
        if (!SupabaseClient.isConfigured || SupabaseClient.accessToken.isNullOrBlank()) {
            return emptyList()
        }
        val result = SupabaseClient.request(
            method = "GET",
            path = "/rest/v1/notices" +
                "?select=id,title,content,type,created_at,starts_at,ends_at,status,audience" +
                "&status=in.(PUBLISHED,SCHEDULED)" +
                "&audience=cs.{DRIVER}" +
                "&order=created_at.desc" +
                "&limit=$limit",
            authed = true,
        )
        if (result.code !in 200..299) {
            Log.w(TAG, "fetch failed ${result.code}: ${result.body.take(200)}")
            return emptyList()
        }
        return runCatching {
            val arr = JSONArray(result.body)
            val now = Instant.now()
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val id = o.optString("id")
                    if (id.isBlank()) continue
                    if (!audienceIncludesDriver(o.opt("audience"))) continue
                    val notice = DriverNotice(
                        id = id,
                        title = o.optString("title").ifBlank { "공지" },
                        content = o.optString("content"),
                        type = o.optString("type", "GENERAL"),
                        createdAt = o.optString("created_at").takeIf { it.isNotBlank() },
                        startsAt = o.optString("starts_at").takeIf { it.isNotBlank() },
                        endsAt = o.optString("ends_at").takeIf { it.isNotBlank() },
                    )
                    if (!inPublishWindow(notice.startsAt, notice.endsAt, now)) continue
                    add(notice)
                }
            }
        }.getOrElse {
            Log.w(TAG, "parse failed: ${it.message}")
            emptyList()
        }
    }

    private fun audienceIncludesDriver(raw: Any?): Boolean {
        return when (raw) {
            is JSONArray -> (0 until raw.length()).any { idx ->
                raw.optString(idx).equals("DRIVER", ignoreCase = true)
            }
            is String -> raw.contains("DRIVER", ignoreCase = true)
            else -> false
        }
    }

    fun inPublishWindow(startsAt: String?, endsAt: String?, now: Instant = Instant.now()): Boolean {
        val startOk = startsAt.isNullOrBlank() || runCatching {
            OffsetDateTime.parse(startsAt).toInstant() <= now
        }.getOrDefault(true)
        val endOk = endsAt.isNullOrBlank() || runCatching {
            OffsetDateTime.parse(endsAt).toInstant() >= now
        }.getOrDefault(true)
        return startOk && endOk
    }

    fun typeLabel(type: String): String = when (type.uppercase()) {
        "URGENT" -> "긴급 공지"
        "IMPORTANT" -> "중요 공지"
        "OPERATION_CHANGE" -> "운행 변경"
        else -> "일반 공지"
    }

    fun isUrgentType(type: String): Boolean =
        type.equals("URGENT", ignoreCase = true) || type.equals("IMPORTANT", ignoreCase = true)

    fun timeLabel(iso: String?): String {
        if (iso.isNullOrBlank()) {
            return java.time.LocalTime.now().format(timeFmt)
        }
        return runCatching {
            OffsetDateTime.parse(iso).atZoneSameInstant(zone).toLocalTime().format(timeFmt)
        }.getOrElse {
            java.time.LocalTime.now().format(timeFmt)
        }
    }

    fun displayDateTime(notice: DriverNotice): String {
        val iso = notice.startsAt ?: notice.createdAt
        if (iso.isNullOrBlank()) {
            return java.time.LocalDateTime.now().format(dateTimeFmt)
        }
        return runCatching {
            OffsetDateTime.parse(iso).atZoneSameInstant(zone).toLocalDateTime().format(dateTimeFmt)
        }.getOrElse {
            java.time.LocalDateTime.now().format(dateTimeFmt)
        }
    }
}
