package com.onda.mju.student.data.remote.repository

import com.onda.mju.student.data.remote.SupabaseClientProvider
import com.onda.mju.student.data.remote.dto.VehicleLocationDto
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
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
 * Reads vehicle location rows from public.vehicle_locations.
 * Keep this layer thin; add realtime later when wiring live maps.
 */
class VehicleLocationRepository {

    suspend fun getLatestLocation(operationId: String): VehicleLocationDto? {
        return SupabaseClientProvider.client
            .from("vehicle_locations")
            .select {
                filter {
                    eq("operation_id", operationId)
                }
                order(column = "recorded_at", order = Order.DESCENDING)
                limit(count = 1)
            }
            .decodeSingleOrNull<VehicleLocationDto>()
    }

    fun observeLocations(operationId: String): Flow<VehicleLocationDto> = channelFlow {
        val client = SupabaseClientProvider.client
        val channel = client.channel("vehicle_locations:$operationId")

        try {
            val changeFlow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = "vehicle_locations"
                filter("operation_id", FilterOperator.EQ, operationId)
            }

            launch {
                changeFlow.collect { action ->
                    send(action.decodeRecord<VehicleLocationDto>())
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
