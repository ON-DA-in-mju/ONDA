package com.onda.mju.student.data.remote.repository

import android.util.Log
import com.onda.mju.student.data.remote.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class ReportViewsRepository {

    companion object {
        private const val TAG = "ONDA_VIEWS"
    }

    @Serializable
    private data class RecordViewParams(
        @SerialName("p_report_id") val reportId: String,
    )

    @Serializable
    private data class ReportViewRow(
        @SerialName("report_id") val reportId: String,
    )

    fun currentUserId(): String? =
        runCatching {
            SupabaseClientProvider.client.auth.currentUserOrNull()?.id
        }.getOrNull()

    /** 현재 사용자가 이미 본 report_id 목록 */
    suspend fun listMyViewedReportIds(): Set<String> {
        val uid = currentUserId() ?: return emptySet()
        return runCatching {
            SupabaseClientProvider.client
                .from("report_views")
                .select(columns = Columns.raw("report_id")) {
                    filter { eq("user_id", uid) }
                }
                .decodeList<ReportViewRow>()
                .map { it.reportId }
                .toSet()
        }.onFailure { e ->
            Log.w(TAG, "listMyViewed failed: ${e.message}")
        }.getOrDefault(emptySet())
    }

    /**
     * 조회 1회 기록. 같은 사용자는 중복 증가하지 않음.
     * @return 갱신된 view_count (실패 시 null)
     */
    suspend fun recordView(reportId: String): Int? {
        val id = reportId.trim()
        if (id.isEmpty()) return null
        return runCatching {
            val count = SupabaseClientProvider.client.postgrest
                .rpc(
                    function = "record_report_view",
                    parameters = RecordViewParams(reportId = id),
                )
                .decodeAs<Int>()
            Log.d(TAG, "recorded view report=$id count=$count")
            count
        }.onFailure { e ->
            Log.w(TAG, "recordView failed report=$id: ${e.message}")
        }.getOrNull()
    }
}
