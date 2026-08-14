package com.onda.mju.student.data.remote.repository

import com.onda.mju.student.data.remote.SupabaseClientProvider
import com.onda.mju.student.data.remote.dto.RouteDetailDto
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

class RouteRepository {

    suspend fun getActiveRoutes(): List<RouteDetailDto> {
        return try {
            SupabaseClientProvider.client
                .from("routes")
                .select(
                    columns = Columns.raw(
                        "id, route_name, direction, description, is_active, start_location, end_location",
                    ),
                ) {
                    filter {
                        eq("is_active", true)
                    }
                }
                .decodeList<RouteDetailDto>()
        } catch (_: Exception) {
            // start_location/end_location may be missing on older DBs.
            SupabaseClientProvider.client
                .from("routes")
                .select(
                    columns = Columns.raw(
                        "id, route_name, direction, description, is_active",
                    ),
                ) {
                    filter {
                        eq("is_active", true)
                    }
                }
                .decodeList<RouteDetailDto>()
        }
    }
}
