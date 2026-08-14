package com.onda.mju.student.data.remote.repository

import com.onda.mju.student.data.remote.SupabaseClientProvider
import com.onda.mju.student.data.remote.dto.OperationDeviceStatusDto
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
 * Reads and observes public.operation_device_status heartbeats per operation.
 */
class OperationDeviceStatusRepository {

    suspend fun getStatus(operationId: String): OperationDeviceStatusDto? {
        return SupabaseClientProvider.client
            .from("operation_device_status")
            .select {
                filter {
                    eq("operation_id", operationId)
                }
                limit(count = 1)
            }
            .decodeSingleOrNull<OperationDeviceStatusDto>()
    }

    /**
     * Observes INSERT and UPDATE on operation_device_status for a single operation.
     * Requires public.operation_device_status in supabase_realtime publication.
     */
    fun observeStatus(operationId: String): Flow<OperationDeviceStatusDto> = channelFlow {
        val client = SupabaseClientProvider.client
        val channel = client.channel("operation_device_status:$operationId")

        try {
            val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "operation_device_status"
                filter("operation_id", FilterOperator.EQ, operationId)
            }

            launch {
                changeFlow.collect { action ->
                    when (action) {
                        is PostgresAction.Insert -> {
                            send(action.decodeRecord<OperationDeviceStatusDto>())
                        }
                        is PostgresAction.Update -> {
                            send(action.decodeRecord<OperationDeviceStatusDto>())
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
