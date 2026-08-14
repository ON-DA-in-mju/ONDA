package com.onda.mju.student.data.remote.repository

import android.util.Log
import com.onda.mju.student.data.remote.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class FavoritesRepository {

    companion object {
        private const val TAG = "ONDA_FAVORITES"
        const val TYPE_ROUTE = "ROUTE"
        const val TYPE_STOP = "STOP"
    }

    @Serializable
    private data class FavoriteRow(
        @SerialName("user_id") val userId: String,
        @SerialName("target_type") val targetType: String,
        @SerialName("target_id") val targetId: String,
    )

    fun currentUserId(): String? =
        runCatching {
            SupabaseClientProvider.client.auth.currentUserOrNull()?.id
        }.getOrNull()

    data class FavoriteIds(
        val routeIds: Set<String>,
        val stopIds: Set<String>,
    )

    suspend fun listMine(): FavoriteIds? {
        val uid = currentUserId() ?: return null
        return runCatching {
            val rows = SupabaseClientProvider.client
                .from("user_favorites")
                .select(columns = Columns.raw("user_id, target_type, target_id")) {
                    filter { eq("user_id", uid) }
                }
                .decodeList<FavoriteRow>()
            FavoriteIds(
                routeIds = rows.filter { it.targetType == TYPE_ROUTE }.map { it.targetId }.toSet(),
                stopIds = rows.filter { it.targetType == TYPE_STOP }.map { it.targetId }.toSet(),
            )
        }.onFailure { e ->
            Log.w(TAG, "listMine failed: ${e.message}")
        }.getOrNull()
    }

    /**
     * 즐겨찾기 전체 교체 (노선 + 정류장).
     * @return true if DB write succeeded
     */
    suspend fun replaceAll(routeIds: Collection<String>, stopIds: Collection<String>): Boolean {
        val uid = currentUserId() ?: return false
        val routes = routeIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val stops = stopIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        return runCatching {
            SupabaseClientProvider.client
                .from("user_favorites")
                .delete {
                    filter { eq("user_id", uid) }
                }
            val rows = routes.map { FavoriteRow(uid, TYPE_ROUTE, it) } +
                stops.map { FavoriteRow(uid, TYPE_STOP, it) }
            if (rows.isNotEmpty()) {
                SupabaseClientProvider.client
                    .from("user_favorites")
                    .insert(rows)
            }
            Log.d(TAG, "replaceAll routes=${routes.size} stops=${stops.size}")
            true
        }.onFailure { e ->
            Log.w(TAG, "replaceAll failed: ${e.message}")
        }.getOrDefault(false)
    }
}
