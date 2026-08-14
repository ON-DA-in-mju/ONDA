package com.onda.mju.student.data.remote.repository

import com.onda.mju.student.data.remote.SupabaseClientProvider
import com.onda.mju.student.data.remote.dto.ReportReactionDto
import com.onda.mju.student.data.remote.dto.ReportReactionPatchDto
import com.onda.mju.student.data.remote.dto.ReportReactionUpsertDto
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import java.time.Instant
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReportReactionsRepository {

    fun currentUserId(): String? =
        runCatching {
            SupabaseClientProvider.client.auth.currentUserOrNull()?.id
        }.getOrNull()

    suspend fun listForReports(reportIds: List<String>): List<ReportReactionDto> {
        if (reportIds.isEmpty()) return emptyList()
        return SupabaseClientProvider.client
            .from("report_reactions")
            .select(
                columns = Columns.raw(
                    "id, report_id, user_id, reaction, created_at, updated_at",
                ),
            ) {
                filter {
                    isIn("report_id", reportIds)
                }
            }
            .decodeList()
    }

    /** 특정 제보/글의 LIKE·DISLIKE 수와 내 반응 (계정별 행 집계) */
    suspend fun summaryForReport(
        reportId: String,
        userId: String?,
    ): ReactionSummary {
        val rows = listForReports(listOf(reportId))
        val likes = rows.count { it.reaction.equals("LIKE", ignoreCase = true) }
        val dislikes = rows.count { it.reaction.equals("DISLIKE", ignoreCase = true) }
        val mine = userId
            ?.let { uid -> rows.firstOrNull { it.userId == uid }?.reaction }
        return ReactionSummary(
            likeCount = likes,
            dislikeCount = dislikes,
            myReaction = mine,
        )
    }

    data class ReactionSummary(
        val likeCount: Int,
        val dislikeCount: Int,
        val myReaction: String?,
    )

    /**
     * targetReaction 이 null 이면 반응 제거.
     * 같은 반응을 다시 누르면 null 을 넘겨 취소하면 된다.
     * unique(report_id, user_id) 이므로 계정마다 1행 → 다른 계정이 누르면 카운트 +1.
     */
    suspend fun setReaction(
        reportId: String,
        userId: String,
        targetReaction: String?,
    ) {
        val existing = SupabaseClientProvider.client
            .from("report_reactions")
            .select(columns = Columns.raw("id, report_id, user_id, reaction")) {
                filter {
                    eq("report_id", reportId)
                    eq("user_id", userId)
                }
                limit(1)
            }
            .decodeList<ReportReactionDto>()
            .firstOrNull()

        if (targetReaction == null) {
            if (existing != null) {
                SupabaseClientProvider.client
                    .from("report_reactions")
                    .delete {
                        filter {
                            eq("report_id", reportId)
                            eq("user_id", userId)
                        }
                    }
            }
            return
        }

        val payload = ReportReactionUpsertDto(
            reportId = reportId,
            userId = userId,
            reaction = targetReaction,
            updatedAt = Instant.now().toString(),
        )

        if (existing == null) {
            SupabaseClientProvider.client
                .from("report_reactions")
                .insert(payload)
        } else {
            SupabaseClientProvider.client
                .from("report_reactions")
                .update(
                    ReportReactionPatchDto(
                        reaction = targetReaction,
                        updatedAt = Instant.now().toString(),
                    ),
                ) {
                    filter {
                        eq("report_id", reportId)
                        eq("user_id", userId)
                    }
                }
        }
    }

    /**
     * 반응 INSERT/UPDATE/DELETE 구독 → 호출측에서 목록 재집계.
     * Requires public.report_reactions in supabase_realtime publication.
     */
    fun observeChanges(): Flow<Unit> = channelFlow {
        val client = SupabaseClientProvider.client
        val channel = client.channel("report_reactions:all")
        try {
            runCatching { client.realtime.connect() }
            val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "report_reactions"
            }
            launch {
                changeFlow.collect { send(Unit) }
            }
            channel.subscribe(blockUntilSubscribed = true)
            awaitCancellation()
        } finally {
            withContext(NonCancellable) {
                runCatching { client.realtime.removeChannel(channel) }
            }
        }
    }
}
