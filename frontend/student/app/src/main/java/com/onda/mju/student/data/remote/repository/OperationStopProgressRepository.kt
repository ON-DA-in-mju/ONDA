package com.onda.mju.student.data.remote.repository

import com.onda.mju.student.data.remote.SupabaseClientProvider
import com.onda.mju.student.data.remote.dto.OperationStopProgressDto
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Reads and observes public.operation_stop_progress per operation.
 */
class OperationStopProgressRepository {

    suspend fun getProgress(operationId: String): OperationStopProgressDto? {
        return SupabaseClientProvider.client
            .from("operation_stop_progress")
            .select {
                filter {
                    eq("operation_id", operationId)
                }
                limit(count = 1)
            }
            .decodeSingleOrNull<OperationStopProgressDto>()
    }

    fun observeProgress(operationId: String): Flow<OperationStopProgressDto> = channelFlow {
        val client = SupabaseClientProvider.client
        val channel = client.channel("operation_stop_progress:$operationId")

        try {
            val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "operation_stop_progress"
                filter("operation_id", FilterOperator.EQ, operationId)
            }

            launch {
                changeFlow.collect { action ->
                    when (action) {
                        is PostgresAction.Insert -> {
                            send(action.decodeRecord<OperationStopProgressDto>())
                        }
                        is PostgresAction.Update -> {
                            send(action.decodeRecord<OperationStopProgressDto>())
                        }
                        else -> Unit
                    }
                }
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
