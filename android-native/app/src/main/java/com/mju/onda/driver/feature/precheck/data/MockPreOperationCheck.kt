package com.mju.onda.driver.feature.precheck.data



import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.outlined.Battery3Bar

import androidx.compose.material.icons.outlined.CloudQueue

import androidx.compose.material.icons.outlined.EnergySavingsLeaf

import androidx.compose.material.icons.outlined.Layers

import androidx.compose.material.icons.outlined.LocationOn

import androidx.compose.material.icons.outlined.MyLocation

import androidx.compose.material.icons.outlined.Settings

import androidx.compose.material.icons.outlined.Wifi

import androidx.compose.ui.graphics.vector.ImageVector



enum class CheckStatus {

    Normal, // 정상

    Caution, // 주의

    ActionRequired, // 조치 필요

}



data class PreCheckItem(

    val id: String,

    val icon: ImageVector,

    val label: String,

    val detail: String,

    val status: CheckStatus,

)



object MockPreOperationCheck {

    const val SCREEN_TITLE = "운행 전 자동 점검"

    const val HEADLINE = "운행 시작 전"

    const val SUBHEADLINE = "필수 항목을 확인해 주세요"

    const val INFO_NOTICE = "필수 항목이 모두 정상이어야\n운행을 시작할 수 있습니다."



    const val RECHECK_LABEL = "다시 점검"

    const val OPEN_SETTINGS_LABEL = "설정 열기"

    const val START_LABEL = "운행 시작"

    const val START_PENDING_TOAST = "운행 시작 화면은 다음 단계에서 연결합니다."

    const val RECHECK_STILL_ISSUES_TOAST = "아직 조치가 필요한 항목이 있습니다."



    const val STATUS_NORMAL = "정상"

    const val STATUS_CAUTION = "주의"

    const val STATUS_ACTION = "조치 필요"



    /** UI 프리뷰용 더미 (실제 화면은 PreCheckDeviceStatus 사용) */

    val initialItems: List<PreCheckItem> = listOf(

        PreCheckItem("location", Icons.Outlined.LocationOn, "위치 권한", "허용됨", CheckStatus.Normal),

        PreCheckItem("precise", Icons.Outlined.MyLocation, "정확한 위치", "사용 중", CheckStatus.Normal),

        PreCheckItem("background", Icons.Outlined.Layers, "백그라운드 위치", "허용됨", CheckStatus.Normal),

        PreCheckItem("gps", Icons.Outlined.Settings, "GPS", "켜짐", CheckStatus.Normal),

        PreCheckItem("network", Icons.Outlined.Wifi, "네트워크", "연결됨", CheckStatus.Normal),

        PreCheckItem("server", Icons.Outlined.CloudQueue, "서버 연결", "연결됨", CheckStatus.Normal),

        PreCheckItem("battery", Icons.Outlined.Battery3Bar, "배터리", "80%", CheckStatus.Normal),

        PreCheckItem("powersave", Icons.Outlined.EnergySavingsLeaf, "절전 모드", "해제됨", CheckStatus.Normal),

    )

}


