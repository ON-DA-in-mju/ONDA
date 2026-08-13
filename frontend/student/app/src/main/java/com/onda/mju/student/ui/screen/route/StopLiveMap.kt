package com.onda.mju.student.ui.screen.route

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.compose.ExperimentalNaverMapApi
import com.naver.maps.map.compose.LocationTrackingMode
import com.naver.maps.map.compose.MapProperties
import com.naver.maps.map.compose.MapUiSettings
import com.naver.maps.map.compose.Marker
import com.naver.maps.map.compose.MarkerState
import com.naver.maps.map.compose.NaverMap
import com.naver.maps.map.compose.rememberCameraPositionState
import com.naver.maps.map.overlay.OverlayImage

private val StopPinIcon = OverlayImage.fromResource(
    com.naver.maps.map.R.drawable.navermap_default_marker_icon_lightblue,
)
private val BusPinIcon = OverlayImage.fromResource(
    com.naver.maps.map.R.drawable.navermap_default_marker_icon_lightblue,
)

/** Relative pin size: selected stop = 2, other stops = 1. */
private val StopPinWidth = 24.dp
private val StopPinHeight = 32.dp
private val OndaBlue = Color(0xFF0041F1)

@OptIn(ExperimentalNaverMapApi::class)
@Composable
fun StopLiveMap(
    stopName: String,
    stopLatitude: Double,
    stopLongitude: Double,
    routeWaypoints: List<StopWaypoint>,
    vehicles: List<LiveVehicle>,
    modifier: Modifier = Modifier,
) {
    val stopLatLng = remember(stopLatitude, stopLongitude) {
        LatLng(stopLatitude, stopLongitude)
    }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition(stopLatLng, 14.5)
    }
    val locationSource = rememberStudentMapLocationSource()
    var locationTrackingMode by remember { mutableStateOf(LocationTrackingMode.NoFollow) }

    val uiSettings = remember {
        MapUiSettings(
            isScrollGesturesEnabled = true,
            isZoomGesturesEnabled = true,
            isTiltGesturesEnabled = false,
            isRotateGesturesEnabled = true,
            isZoomControlEnabled = true,
            isScaleBarEnabled = true,
            // SDK 기본 버튼 대신 우측 하단 커스텀 버튼 사용
            isLocationButtonEnabled = false,
        )
    }
    val mapProperties = remember(locationTrackingMode) {
        MapProperties(locationTrackingMode = locationTrackingMode)
    }

    androidx.compose.runtime.LaunchedEffect(stopLatLng) {
        if (locationTrackingMode == LocationTrackingMode.None ||
            locationTrackingMode == LocationTrackingMode.NoFollow
        ) {
            cameraPositionState.move(
                CameraUpdate.toCameraPosition(CameraPosition(stopLatLng, 14.5)),
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        NaverMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = uiSettings,
            properties = mapProperties,
            locationSource = locationSource,
        ) {
            routeWaypoints.forEach { waypoint ->
                val isSelected = waypoint.name == stopName
                val scale = if (isSelected) 2f else 1f
                Marker(
                    state = MarkerState(
                        position = LatLng(waypoint.latitude, waypoint.longitude),
                    ),
                    captionText = waypoint.name,
                    captionTextSize = if (isSelected) 13.sp else 11.sp,
                    zIndex = if (isSelected) 10 else 0,
                    icon = StopPinIcon,
                    width = StopPinWidth * scale,
                    height = StopPinHeight * scale,
                )
            }

            vehicles.forEach { vehicle ->
                val lat = vehicle.latitude ?: return@forEach
                val lng = vehicle.longitude ?: return@forEach
                Marker(
                    state = MarkerState(position = LatLng(lat, lng)),
                    captionText = vehicle.label,
                    captionTextSize = 12.sp,
                    zIndex = 20,
                    icon = BusPinIcon,
                    width = StopPinWidth,
                    height = StopPinHeight,
                )
            }
        }

        SmallFloatingActionButton(
            onClick = {
                locationTrackingMode = LocationTrackingMode.Follow
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 12.dp),
            shape = CircleShape,
            containerColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.MyLocation,
                contentDescription = "내 위치로 이동",
                tint = OndaBlue,
            )
        }
    }
}
