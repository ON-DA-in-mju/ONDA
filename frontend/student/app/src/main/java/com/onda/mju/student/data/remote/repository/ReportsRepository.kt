package com.onda.mju.student.data.remote.repository

import com.onda.mju.student.data.remote.SupabaseClientProvider
import com.onda.mju.student.data.remote.dto.ReportDto
import com.onda.mju.student.data.remote.dto.ReportInsertDto
import com.onda.mju.student.data.remote.dto.ReportUpdateDto
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order

class ReportsRepository {

    fun currentUserId(): String? =
        runCatching {
            SupabaseClientProvider.client.auth.currentUserOrNull()?.id
        }.getOrNull()

    private val reportColumns = Columns.raw(
        "id, user_id, title, content, status, source, board_type, category, created_at, updated_at, view_count",
    )
    private val reportColumnsNoView = Columns.raw(
        "id, user_id, title, content, status, source, board_type, category, created_at, updated_at",
    )

    /** 상황 제보만 (board_type = REPORT) */
    suspend fun listStudentReports(limit: Int = 50): List<ReportDto> {
        return listByBoardType(boardType = "REPORT", limit = limit)
    }

    /** 소통 글쓰기만 (board_type = POST) */
    suspend fun listStudentPosts(limit: Int = 50): List<ReportDto> {
        return listByBoardType(boardType = "POST", limit = limit)
    }

    private suspend fun listByBoardType(boardType: String, limit: Int): List<ReportDto> {
        val withBoardType: suspend (Columns) -> List<ReportDto> = { columns ->
            SupabaseClientProvider.client
                .from("reports")
                .select(columns = columns) {
                    filter {
                        eq("source", "STUDENT")
                        eq("board_type", boardType)
                    }
                    order(column = "created_at", order = Order.DESCENDING)
                    limit(count = limit.toLong())
                }
                .decodeList()
        }
        return try {
            withBoardType(reportColumns)
        } catch (_: Exception) {
            try {
                withBoardType(reportColumnsNoView)
            } catch (_: Exception) {
                // board_type 컬럼 미적용 DB 대비: REPORT 만 기존 방식으로 조회
                if (boardType != "REPORT") return emptyList()
                SupabaseClientProvider.client
                    .from("reports")
                    .select(
                        columns = Columns.raw(
                            "id, user_id, title, content, status, source, category, created_at, updated_at",
                        ),
                    ) {
                        filter {
                            eq("source", "STUDENT")
                        }
                        order(column = "created_at", order = Order.DESCENDING)
                        limit(count = limit.toLong())
                    }
                    .decodeList()
            }
        }
    }

    suspend fun listMyReports(userId: String, limit: Int = 50): List<ReportDto> {
        return try {
            SupabaseClientProvider.client
                .from("reports")
                .select(columns = reportColumns) {
                    filter {
                        eq("source", "STUDENT")
                        eq("board_type", "REPORT")
                        eq("user_id", userId)
                    }
                    order(column = "created_at", order = Order.DESCENDING)
                    limit(count = limit.toLong())
                }
                .decodeList()
        } catch (_: Exception) {
            SupabaseClientProvider.client
                .from("reports")
                .select(
                    columns = Columns.raw(
                        "id, user_id, title, content, status, source, category, created_at, updated_at",
                    ),
                ) {
                    filter {
                        eq("source", "STUDENT")
                        eq("user_id", userId)
                    }
                    order(column = "created_at", order = Order.DESCENDING)
                    limit(count = limit.toLong())
                }
                .decodeList()
        }
    }

    suspend fun create(report: ReportInsertDto): ReportDto {
        return SupabaseClientProvider.client
            .from("reports")
            .insert(report) {
                select()
            }
            .decodeSingle()
    }

    suspend fun update(id: String, patch: ReportUpdateDto): ReportDto {
        return SupabaseClientProvider.client
            .from("reports")
            .update(patch) {
                filter { eq("id", id) }
                select()
            }
            .decodeSingle()
    }

    suspend fun delete(id: String) {
        SupabaseClientProvider.client
            .from("reports")
            .delete {
                filter { eq("id", id) }
            }
    }
}
