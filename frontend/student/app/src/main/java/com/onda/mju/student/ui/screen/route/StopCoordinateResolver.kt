package com.onda.mju.student.ui.screen.route

import com.onda.mju.student.data.remote.dto.StopDto
import com.onda.mju.student.data.route.RouteStopCatalog

typealias StopCoordinateMap = Map<String, Pair<Double, Double>>

object StopCoordinateResolver {

    /**
     * 표기만 다른 경우의 안전 alias.
     * 서로 다른 정류장끼리 섞지 않는다 (채플관↔버스관리사무소 금지).
     */
    private val nameAliases: Map<String, String> = mapOf(
        "명지대역 사거리" to "명지대역 사거리 정류장",
        "용인 CGV" to "용인CGV",
    )

    fun fromStops(stops: List<StopDto>): StopCoordinateMap =
        stops.associate { it.stopName to (it.latitude to it.longitude) }

    /** DB `stops` 맵에서만 조회. 하드코딩 좌표 없음. */
    fun lookup(stopName: String, dbCoordinates: StopCoordinateMap): Pair<Double, Double>? {
        if (dbCoordinates.isEmpty()) return null
        nameAliases[stopName]?.let { dbCoordinates[it] }?.let { return it }
        dbCoordinates[stopName]?.let { return it }
        return dbCoordinates.entries.firstOrNull { (key, _) ->
            key.equals(stopName, ignoreCase = true)
        }?.value
    }
}
