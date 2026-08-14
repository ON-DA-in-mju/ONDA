package com.onda.mju.student.data.remote.repository

import android.util.Log
import com.onda.mju.student.data.remote.SupabaseClientProvider
import com.onda.mju.student.data.remote.dto.NoticeDto
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import java.time.Instant
import java.time.OffsetDateTime
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class NoticesRepository {

    companion object {
        private const val TAG = "ONDA_NOTICES"
        private val channelSeq = AtomicLong(0)

        /** 사용자 DB 스키마 기준 (is_push 없음) */
        private val COLUMNS_FULL = Columns.raw(
            "id, title, content, type, status, audience, starts_at, ends_at, created_at, updated_at, view_count",
        )
        private val COLUMNS_NO_AUDIENCE = Columns.raw(
            "id, title, content, type, status, starts_at, ends_at, created_at, updated_at",
        )
        private val COLUMNS_MIN = Columns.raw(
            "id, title, content, type, status, created_at, updated_at",
        )
    }

    @Serializable
    private data class IncrementViewParams(
        @SerialName("p_notice_id") val noticeId: String,
    )

    /**
     * 학생 앱에 노출할 공지.
     * RLS + 클라이언트에서 STUDENT / PUBLISHED|SCHEDULED / 게시 기간 필터.
     */
    suspend fun listForStudent(limit: Int = 50): List<NoticeDto> {
        val rows = fetchRows(limit)
        val now = Instant.now()
        val visible = rows.filter { row ->
            audienceIncludesStudent(row.audience) &&
                row.status.uppercase() in setOf("PUBLISHED", "SCHEDULED") &&
                inPublishWindow(row.startsAt, row.endsAt, now)
        }
        Log.d(
            TAG,
            "fetch raw=${rows.size}, visible=${visible.size}, " +
                "titles=${visible.map { it.title }.joinToString()}",
        )
        return visible
    }

    /**
     * 공지 상세 열람 시 view_count +1.
     * RPC: public.increment_notice_view_count (SECURITY DEFINER)
     */
    suspend fun incrementViewCount(noticeId: String): Int? {
        val id = noticeId.trim()
        if (id.isEmpty()) return null
        return runCatching {
            val result = SupabaseClientProvider.client.postgrest
                .rpc(
                    function = "increment_notice_view_count",
                    parameters = IncrementViewParams(noticeId = id),
                )
                .decodeAs<Int>()
            Log.d(TAG, "view_count incremented notice=$id → $result")
            result
        }.onFailure { e ->
            Log.w(TAG, "view_count increment failed notice=$id: ${e.message}")
        }.getOrNull()
    }

    private suspend fun fetchRows(limit: Int): List<NoticeDto> {
        val attempts: List<Pair<String, Columns>> = listOf(
            "full+status" to COLUMNS_FULL,
            "no-audience+status" to COLUMNS_NO_AUDIENCE,
            "min+status" to COLUMNS_MIN,
            "full-all" to COLUMNS_FULL,
            "no-audience-all" to COLUMNS_NO_AUDIENCE,
            "min-all" to COLUMNS_MIN,
        )

        var lastError: Throwable? = null
        for ((label, columns) in attempts) {
            val withStatus = label.endsWith("+status")
            val result = runCatching {
                SupabaseClientProvider.client
                    .from("notices")
                    .select(columns = columns) {
                        if (withStatus) {
                            filter {
                                isIn("status", listOf("PUBLISHED", "SCHEDULED"))
                            }
                        }
                        order(column = "created_at", order = Order.DESCENDING)
                        limit(count = limit.toLong())
                    }
                    .decodeList<NoticeDto>()
            }
            if (result.isSuccess) {
                val rows = result.getOrThrow()
                Log.d(TAG, "select ok via=$label count=${rows.size}")
                return rows
            }
            lastError = result.exceptionOrNull()
            Log.w(TAG, "select failed via=$label: ${lastError?.message}")
        }
        throw lastError ?: IllegalStateException("notices select failed")
    }

    /**
     * notices 변경(INSERT/UPDATE/DELETE)을 구독한다.
     * 이벤트마다 Unit 을 흘려 호출측에서 listForStudent 로 재조회한다.
     * 끊기면 자동 재연결한다. (publication: public.notices)
     */
    fun observeChanges(): Flow<Unit> = channelFlow {
        val client = SupabaseClientProvider.client

        while (isActive) {
            val channelName = "notices:student:${channelSeq.incrementAndGet()}"
            val channel = client.channel(channelName)
            try {
                runCatching { client.realtime.connect() }
                    .onFailure { Log.w(TAG, "realtime connect: ${it.message}") }

                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "notices"
                }

                val collector = launch {
                    changeFlow.collect { action ->
                        Log.d(TAG, "realtime ${action::class.simpleName} → refresh")
                        send(Unit)
                    }
                }

                channel.subscribe(blockUntilSubscribed = true)
                Log.d(TAG, "realtime subscribed channel=$channelName")
                // 구독 직후 1회 동기화
                send(Unit)

                // 구독 유지. 외부 cancel 또는 예외 시 finally 에서 정리
                collector.join()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "realtime loop error, retry in 3s: ${e.message}")
                delay(3_000L)
            } finally {
                withContext(NonCancellable) {
                    runCatching { client.realtime.removeChannel(channel) }
                }
            }
        }
    }

    private fun audienceIncludesStudent(audience: List<String>?): Boolean {
        // audience 미선택/null → RLS 가 이미 학생용만 줬다고 가정
        if (audience == null) return true
        if (audience.isEmpty()) return false
        return audience.any { token ->
            token.contains("STUDENT", ignoreCase = true) || token.contains("학생")
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
}
