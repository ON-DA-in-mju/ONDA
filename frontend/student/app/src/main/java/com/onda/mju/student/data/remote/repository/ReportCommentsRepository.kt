package com.onda.mju.student.data.remote.repository

import com.onda.mju.student.data.remote.SupabaseClientProvider
import com.onda.mju.student.data.remote.dto.ReportCommentContentPatchDto
import com.onda.mju.student.data.remote.dto.ReportCommentDto
import com.onda.mju.student.data.remote.dto.ReportCommentInsertDto
import com.onda.mju.student.data.remote.dto.ReportCommentSoftDeletePatchDto
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import java.time.Instant

class ReportCommentsRepository {

    private val columns = Columns.raw(
        "id, report_id, user_id, content, is_deleted, created_at, updated_at",
    )

    fun currentUserId(): String? =
        runCatching {
            SupabaseClientProvider.client.auth.currentUserOrNull()?.id
        }.getOrNull()

    suspend fun listForReport(reportId: String): List<ReportCommentDto> {
        return try {
            SupabaseClientProvider.client
                .from("report_comments")
                .select(columns = columns) {
                    filter { eq("report_id", reportId) }
                    order(column = "created_at", order = Order.ASCENDING)
                }
                .decodeList()
        } catch (_: Exception) {
            // is_deleted 컬럼 미적용 DB 대비
            SupabaseClientProvider.client
                .from("report_comments")
                .select(
                    columns = Columns.raw(
                        "id, report_id, user_id, content, created_at, updated_at",
                    ),
                ) {
                    filter { eq("report_id", reportId) }
                    order(column = "created_at", order = Order.ASCENDING)
                }
                .decodeList()
        }
    }

    suspend fun listForReports(reportIds: List<String>): List<ReportCommentDto> {
        if (reportIds.isEmpty()) return emptyList()
        return try {
            SupabaseClientProvider.client
                .from("report_comments")
                .select(columns = columns) {
                    filter { isIn("report_id", reportIds) }
                }
                .decodeList()
        } catch (_: Exception) {
            SupabaseClientProvider.client
                .from("report_comments")
                .select(
                    columns = Columns.raw(
                        "id, report_id, user_id, content, created_at, updated_at",
                    ),
                ) {
                    filter { isIn("report_id", reportIds) }
                }
                .decodeList()
        }
    }

    suspend fun create(comment: ReportCommentInsertDto): ReportCommentDto {
        return SupabaseClientProvider.client
            .from("report_comments")
            .insert(comment) {
                select()
            }
            .decodeSingle()
    }

    suspend fun update(id: String, content: String): ReportCommentDto {
        return SupabaseClientProvider.client
            .from("report_comments")
            .update(
                ReportCommentContentPatchDto(
                    content = content.trim(),
                    updatedAt = Instant.now().toString(),
                ),
            ) {
                filter { eq("id", id) }
                select()
            }
            .decodeSingle()
    }

    /** 행은 남기고 is_deleted = true (화면에 "삭제된 댓글입니다.") */
    suspend fun softDelete(id: String): ReportCommentDto {
        return try {
            SupabaseClientProvider.client
                .from("report_comments")
                .update(
                    ReportCommentSoftDeletePatchDto(
                        isDeleted = true,
                        updatedAt = Instant.now().toString(),
                    ),
                ) {
                    filter { eq("id", id) }
                    select()
                }
                .decodeSingle()
        } catch (e: Exception) {
            // is_deleted 미적용 DB: 내용만 플레이스홀더로 바꿔 표시 가능하게 한다.
            runCatching {
                SupabaseClientProvider.client
                    .from("report_comments")
                    .update(
                        ReportCommentContentPatchDto(
                            content = "삭제된 댓글입니다.",
                            updatedAt = Instant.now().toString(),
                        ),
                    ) {
                        filter { eq("id", id) }
                        select()
                    }
                    .decodeSingle<ReportCommentDto>()
                    .copy(isDeleted = true, content = "삭제된 댓글입니다.")
            }.getOrElse { throw e }
        }
    }
}
