package com.onda.mju.student.data.remote.repository

import com.onda.mju.student.data.remote.SupabaseClientProvider
import com.onda.mju.student.data.remote.dto.StopDto
import io.github.jan.supabase.postgrest.from

class StopRepository {

    suspend fun getAllStops(): List<StopDto> {
        return SupabaseClientProvider.client
            .from("stops")
            .select {
                // id, stop_name, latitude, longitude
            }
            .decodeList<StopDto>()
    }
}
