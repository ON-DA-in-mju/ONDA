package com.onda.mju.student.data.remote.repository

import com.onda.mju.student.data.remote.SupabaseClientProvider
import com.onda.mju.student.data.remote.dto.BusDto
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

class BusRepository {

    suspend fun getActiveBuses(): List<BusDto> {
        val active = try {
            SupabaseClientProvider.client
                .from("buses")
                .select(
                    columns = Columns.raw("id, bus_name, vehicle_number"),
                ) {
                    filter {
                        eq("status", "ACTIVE")
                    }
                }
                .decodeList<BusDto>()
        } catch (_: Exception) {
            emptyList()
        }
        if (active.isNotEmpty()) return active

        // status 값이 다르거나 필터가 실패하면 전체 조회로 대체
        return SupabaseClientProvider.client
            .from("buses")
            .select(columns = Columns.raw("id, bus_name, vehicle_number"))
            .decodeList()
    }
}

fun BusDto.displayLabel(): String {
    val name = busName?.trim().orEmpty()
    val plate = vehicleNumber?.trim().orEmpty()
    return when {
        name.isNotBlank() && plate.isNotBlank() -> "$name ($plate)"
        name.isNotBlank() -> name
        plate.isNotBlank() -> plate
        else -> "차량"
    }
}
