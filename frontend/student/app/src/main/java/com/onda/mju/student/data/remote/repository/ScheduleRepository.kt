package com.onda.mju.student.data.remote.repository

import com.onda.mju.student.data.remote.SupabaseClientProvider
import com.onda.mju.student.data.remote.dto.ScheduleDetailDto
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

class ScheduleRepository {

    suspend fun getSchedulesWithRoutes(): List<ScheduleDetailDto> {
        val columns = Columns.raw(
            """
            id,
            route_id,
            departure_time,
            weekday,
            semester,
            routes (
              id,
              route_name,
              direction,
              description,
              is_active,
              start_location,
              end_location
            )
            """.trimIndent(),
        )
        return try {
            SupabaseClientProvider.client
                .from("schedules")
                .select(columns = columns)
                .decodeList<ScheduleDetailDto>()
        } catch (_: Exception) {
            val fallback = Columns.raw(
                """
                id,
                route_id,
                departure_time,
                weekday,
                semester,
                routes (
                  id,
                  route_name,
                  direction,
                  description,
                  is_active
                )
                """.trimIndent(),
            )
            SupabaseClientProvider.client
                .from("schedules")
                .select(columns = fallback)
                .decodeList<ScheduleDetailDto>()
        }
    }
}
