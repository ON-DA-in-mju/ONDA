package com.onda.mju.student.ui.screen.route

import com.onda.mju.student.data.remote.dto.StopDto

typealias StopCoordinateMap = Map<String, Pair<Double, Double>>

object StopCoordinateResolver {

    /** UI 정류장명 → DB stops.stop_name */
    private val nameAliases: Map<String, String> = mapOf(
        "명지대역 사거리" to "명지대역 사거리 정류장",
        "용인 CGV" to "용인CGV",
    )

    fun fromStops(stops: List<StopDto>): StopCoordinateMap =
        stops.associate { it.stopName to (it.latitude to it.longitude) }

    fun lookup(stopName: String, dbCoordinates: StopCoordinateMap): Pair<Double, Double>? {
        if (dbCoordinates.isNotEmpty()) {
            nameAliases[stopName]?.let { dbCoordinates[it] }?.let { return it }
            dbCoordinates[stopName]?.let { return it }
            dbCoordinates.entries.firstOrNull { (key, _) ->
                key.equals(stopName, ignoreCase = true) ||
                    key.contains(stopName) ||
                    stopName.contains(key)
            }?.value?.let { return it }
        }
        return stopCoordinatesByName[stopName]
    }
}
