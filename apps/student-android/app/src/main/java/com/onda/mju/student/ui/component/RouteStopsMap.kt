package com.onda.mju.student.ui.component

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.compose.ExperimentalNaverMapApi
import com.naver.maps.map.compose.MapUiSettings
import com.naver.maps.map.compose.Marker
import com.naver.maps.map.compose.MarkerState
import com.naver.maps.map.compose.NaverMap
import com.naver.maps.map.compose.PathOverlay
import com.naver.maps.map.compose.rememberCameraPositionState
import com.naver.maps.map.overlay.OverlayImage
import com.onda.mju.student.BuildConfig
import com.onda.mju.student.R
import com.onda.mju.student.ui.screen.route.StopWaypoint
import com.onda.mju.student.ui.screen.route.resolveStopSelection
import com.onda.mju.student.ui.screen.route.stopCoordinatesByName

private const val TAG = "ONDA_MAP"
private val OndaBlue = Color(0xFF0041F1)
private val BodyGray = Color(0xFF6B7280)
private val SoftBlue = Color(0xFFEDF4FE)

data class RouteMapStop(
    val stopId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val orderIndex: Int,
)

/**
 * Builds ordered map stops for the selected route/direction.
 * Uses the same resolve path as ETA / header stop name (stopId + routeId).
 * Stops without real coordinates are skipped (no arbitrary fallback pins).
 */
fun routeMapStopsForSelection(
    stopId: String,
    routeId: String?,
): List<RouteMapStop> {
    val resolved = resolveStopSelection(stopId, routeId) ?: return emptyList()
    return resolved.waypoints.mapIndexedNotNull { index, waypoint ->
        toRouteMapStop(waypoint, index)
    }
}

private fun toRouteMapStop(waypoint: StopWaypoint, orderIndex: Int): RouteMapStop? {
    val coords = stopCoordinatesByName[waypoint.name]
    if (coords == null) {
        Log.w(TAG, "Missing lat/lng for stop id=${waypoint.id}, name=${waypoint.name}")
        return null
    }
    if (!isPlausibleKoreaCoord(coords.first, coords.second)) {
        Log.w(
            TAG,
            "Implausible lat/lng for stop id=${waypoint.id}, name=${waypoint.name}: " +
                "${coords.first}, ${coords.second}",
        )
        return null
    }
    return RouteMapStop(
        stopId = waypoint.id,
        name = waypoint.name,
        latitude = coords.first,
        longitude = coords.second,
        orderIndex = orderIndex,
    )
}

private fun isPlausibleKoreaCoord(lat: Double, lng: Double): Boolean =
    lat in 33.0..39.5 && lng in 124.0..132.0

private fun isNaverMapConfigured(): Boolean {
    val id = BuildConfig.NAVER_MAP_CLIENT_ID.trim()
    return id.isNotEmpty() && !id.contains("YOUR_", ignoreCase = true)
}

/**
 * Naver Map showing current route-direction stops + polyline.
 * Map instance stays stable across ETA/GPS tick recompositions; only overlays update.
 */
@OptIn(ExperimentalNaverMapApi::class)
@Composable
fun RouteStopsMap(
    stopId: String,
    routeId: String?,
    modifier: Modifier = Modifier,
) {
    // Keyed only by navigation selection — not by vehicle/ETA clocks.
    val mapStops = remember(stopId, routeId) {
        routeMapStopsForSelection(stopId, routeId)
    }
    val selectedStopId = remember(stopId, routeId) {
        resolveStopSelection(stopId, routeId)?.waypoint?.id
    }
    val cameraPositionState = rememberCameraPositionState()
    var hasFittedBounds by remember(stopId, routeId) { mutableStateOf(false) }
    val mapConfigured = remember { isNaverMapConfigured() }

    val uiSettings = remember {
        MapUiSettings(
            isZoomControlEnabled = false,
            isCompassEnabled = false,
            isScaleBarEnabled = false,
            isLocationButtonEnabled = false,
            isLogoClickEnabled = false,
        )
    }

    LaunchedEffect(mapStops, mapConfigured) {
        if (!mapConfigured || hasFittedBounds || mapStops.isEmpty()) return@LaunchedEffect
        val update = if (mapStops.size == 1) {
            val only = mapStops.first()
            CameraUpdate.scrollAndZoomTo(LatLng(only.latitude, only.longitude), 15.0)
        } else {
            val bounds = LatLngBounds.Builder().apply {
                mapStops.forEach { include(LatLng(it.latitude, it.longitude)) }
            }.build()
            // Padding keeps selected stop readable while showing the full route.
            CameraUpdate.fitBounds(bounds, 72)
        }
        cameraPositionState.move(update)
        hasFittedBounds = true
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SoftBlue),
    ) {
        when {
            !mapConfigured -> {
                Text(
                    text = "네이버 지도 키가 없습니다.\nlocal.properties에 NAVER_MAP_CLIENT_ID를 설정하세요.",
                    color = BodyGray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp),
                )
            }
            mapStops.isEmpty() -> {
                Text(
                    text = "정류장 위치 정보를 불러올 수 없습니다.",
                    color = BodyGray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp),
                )
            }
            else -> {
                NaverMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = uiSettings,
                    onMapClick = { _, _ -> },
                ) {
                    if (mapStops.size >= 2) {
                        PathOverlay(
                            coords = mapStops.map { LatLng(it.latitude, it.longitude) },
                            width = 5.dp,
                            color = OndaBlue.copy(alpha = 0.75f),
                            outlineWidth = 0.dp,
                        )
                    }

                    mapStops.forEach { stop ->
                        val selected = stop.stopId == selectedStopId
                        Marker(
                            state = MarkerState(
                                position = LatLng(stop.latitude, stop.longitude),
                            ),
                            icon = OverlayImage.fromResource(
                                if (selected) {
                                    R.drawable.ic_map_stop_selected
                                } else {
                                    R.drawable.ic_map_stop_default
                                },
                            ),
                            width = if (selected) 34.dp else 22.dp,
                            height = if (selected) 34.dp else 22.dp,
                            captionText = stop.name,
                            captionColor = if (selected) OndaBlue else Color(0xFF374151),
                            captionTextSize = if (selected) 12.sp else 11.sp,
                            zIndex = if (selected) 100 else 10,
                            onClick = {
                                // Caption already shows the name; do not change selectedStop.
                                true
                            },
                        )
                    }
                }
            }
        }
    }
}
