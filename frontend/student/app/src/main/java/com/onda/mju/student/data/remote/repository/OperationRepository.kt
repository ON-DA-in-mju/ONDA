package com.onda.mju.student.data.remote.repository

import com.onda.mju.student.data.remote.SupabaseClientProvider
import com.onda.mju.student.data.remote.dto.OperationDto
import com.onda.mju.student.data.remote.dto.OperationRealtimeDto
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeOldRecord
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Reads operation rows from public.operations.
 * Keep this layer thin; add filters/joins later when wiring UI.
 */
class OperationRepository {

    suspend fun getOperations(): List<OperationDto> {
        val today = LocalDate.now(ZoneId.of("Asia/Seoul"))
            .format(DateTimeFormatter.ISO_LOCAL_DATE)
        val columns = Columns.raw(
            """
            id,
            operation_date,
            status,
            started_at,
            ended_at,
            schedule_id,
            bus_id,
            schedule:schedules (
              id,
              departure_time,
              route_id,
              route:routes (
                id,
                route_name
              )
            ),
            bus:buses (
              id,
              bus_name,
              vehicle_number
            )
            """.trimIndent(),
        )
        return SupabaseClientProvider.client
            .from("operations")
            .select(columns = columns) {
                filter {
                    eq("operation_date", today)
                }
            }
            .decodeList<OperationDto>()
    }

    /**
     * Observes UPDATE events on public.operations for a single operation_date (Asia/Seoul day).
     * Nested schedule/bus relations are not included in Realtime payloads.
     */
    fun observeOperationUpdates(operationDate: String): Flow<OperationRealtimeUpdate> = channelFlow {
        val client = SupabaseClientProvider.client
        val channel = client.channel("operations:$operationDate")

        try {
            val changeFlow = channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                table = "operations"
                filter("operation_date", FilterOperator.EQ, operationDate)
            }

            launch {
                changeFlow.collect { action ->
                    val updated = action.decodeRecord<OperationRealtimeDto>()
                    val previous = try {
                        action.decodeOldRecord<OperationRealtimeDto>()
                    } catch (_: Exception) {
                        null
                    }
                    send(
                        OperationRealtimeUpdate(
                            previousStatus = previous?.status,
                            record = updated,
                        ),
                    )
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

data class OperationRealtimeUpdate(
    val previousStatus: String?,
    val record: OperationRealtimeDto,
)
