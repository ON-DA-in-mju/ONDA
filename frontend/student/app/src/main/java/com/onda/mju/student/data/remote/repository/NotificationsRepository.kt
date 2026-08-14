package com.onda.mju.student.data.remote.repository

import com.onda.mju.student.data.remote.SupabaseClientProvider
import com.onda.mju.student.data.remote.dto.NotificationDto
import com.onda.mju.student.data.remote.dto.NotificationReadUpdateDto
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationsRepository {

    fun currentUserId(): String? =
        runCatching {
            SupabaseClientProvider.client.auth.currentUserOrNull()?.id
        }.getOrNull()

    suspend fun listMine(limit: Int = 50): List<NotificationDto> {
        val uid = currentUserId() ?: return emptyList()
        return SupabaseClientProvider.client
            .from("notifications")
            .select(
                columns = Columns.raw(
                    "id, user_id, title, message, type, is_read, created_at",
                ),
            ) {
                filter {
                    eq("user_id", uid)
                }
                order(column = "created_at", order = Order.DESCENDING)
                limit(count = limit.toLong())
            }
            .decodeList()
    }

    suspend fun markRead(id: String) {
        SupabaseClientProvider.client
            .from("notifications")
            .update(NotificationReadUpdateDto(isRead = true)) {
                filter { eq("id", id) }
            }
    }

    suspend fun markAllRead() {
        val uid = currentUserId() ?: return
        SupabaseClientProvider.client
            .from("notifications")
            .update(NotificationReadUpdateDto(isRead = true)) {
                filter {
                    eq("user_id", uid)
                    eq("is_read", false)
                }
            }
    }

    /** Requires public.notifications in supabase_realtime publication. */
    fun observeChanges(): Flow<Unit> = channelFlow {
        val client = SupabaseClientProvider.client
        val uid = currentUserId()
        val channel = client.channel("notifications:mine")

        try {
            val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "notifications"
                if (!uid.isNullOrBlank()) {
                    filter("user_id", io.github.jan.supabase.postgrest.query.filter.FilterOperator.EQ, uid)
                }
            }

            launch {
                changeFlow.collect { send(Unit) }
            }

            channel.subscribe()
            awaitCancellation()
        } finally {
            withContext(NonCancellable) {
                client.realtime.removeChannel(channel)
            }
        }
    }
}
