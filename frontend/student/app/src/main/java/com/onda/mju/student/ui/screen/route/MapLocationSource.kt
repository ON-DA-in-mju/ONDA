package com.onda.mju.student.ui.screen.route

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.naver.maps.map.LocationSource
import com.naver.maps.map.compose.ExperimentalNaverMapApi
import com.naver.maps.map.location.FusedLocationSource

@OptIn(ExperimentalNaverMapApi::class)
@Composable
fun rememberStudentMapLocationSource(): LocationSource {
    val context = LocalContext.current

    var permissionGranted by remember {
        mutableStateOf(context.hasLocationPermission())
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        permissionGranted =
            result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    val locationSource = remember(context) {
        object : FusedLocationSource(context) {
            override fun hasPermissions(): Boolean = permissionGranted

            override fun onPermissionRequest() {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                )
            }
        }
    }

    LaunchedEffect(permissionGranted) {
        if (permissionGranted) {
            locationSource.onPermissionGranted()
        }
    }

    return locationSource
}

private fun Context.hasLocationPermission(): Boolean {
    val fine = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
}
