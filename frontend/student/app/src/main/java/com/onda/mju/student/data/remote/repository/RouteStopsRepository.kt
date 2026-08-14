package com.onda.mju.student.data.remote.repository

import com.onda.mju.student.data.remote.SupabaseClientProvider
import com.onda.mju.student.data.remote.dto.RouteStopRowDto
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

data class RouteStopInfo(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val order: Int,
    val routeName: String,
)

class RouteStopsRepository {

    suspend fun getStopsForRouteName(routeName: String): List<RouteStopInfo> {
        val key = routeName.trim()
        if (key.isBlank()) return emptyList()
        val columns = Columns.raw(
            "stop_order,stop_id,expected_minutes,stops(id,stop_name,latitude,longitude),routes!inner(id,route_name)",
        )
        val rows = SupabaseClientProvider.client
            .from("route_stops")
            .select(columns = columns) {
                filter {
                    eq("routes.route_name", key)
                }
            }
            .decodeList<RouteStopRowDto>()
        return rows.toStopInfos().sortedBy { it.order }
    }

    suspend fun getAllRouteStops(): List<RouteStopInfo> {
        val columns = Columns.raw(
            "stop_order,stop_id,expected_minutes,stops(id,stop_name,latitude,longitude),routes(id,route_name)",
        )
        val rows = SupabaseClientProvider.client
            .from("route_stops")
            .select(columns = columns)
            .decodeList<RouteStopRowDto>()
        return rows.toStopInfos().sortedWith(compareBy({ it.routeName }, { it.order }))
    }

    private fun List<RouteStopRowDto>.toStopInfos(): List<RouteStopInfo> =
        mapNotNull { row ->
            val stop = row.stops ?: return@mapNotNull null
            val name = stop.stopName.trim()
            if (name.isBlank()) return@mapNotNull null
            RouteStopInfo(
                id = stop.id.ifBlank { row.stopId.orEmpty().ifBlank { "stop-${row.stopOrder}" } },
                name = name,
                lat = stop.latitude,
                lng = stop.longitude,
                order = row.stopOrder,
                routeName = row.routes?.routeName?.trim().orEmpty(),
            )
        }
}
