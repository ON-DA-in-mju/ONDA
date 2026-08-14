package com.mju.onda.driver.feature.precheck.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.EnergySavingsLeaf
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.ui.graphics.vector.ImageVector

data class CompletedCheckItem(
    val icon: ImageVector,
    val label: String,
    val detail: String,
)

object MockPreCheckComplete {
    const val SCREEN_TITLE = "자동 점검 완료"
    const val HEADLINE = "운행을 시작할 준비가 완료되었습니다."
    const val SUBTITLE = "모든 필수 항목이 정상적으로 확인되었습니다."
    const val START_LABEL = "운행 시작"
    const val BACK_LABEL = "이전으로"
    const val START_PENDING_TOAST = "운행 시작 화면은 다음 단계에서 연결합니다."

    val items: List<CompletedCheckItem> = listOf(
        CompletedCheckItem(Icons.Outlined.LocationOn, "위치 권한", "허용됨"),
        CompletedCheckItem(Icons.Outlined.MyLocation, "정확한 위치", "사용중"),
        CompletedCheckItem(Icons.Outlined.Layers, "백그라운드 위치", "허용됨"),
        CompletedCheckItem(Icons.Outlined.GpsFixed, "GPS", "켜짐"),
        CompletedCheckItem(Icons.Outlined.Wifi, "네트워크", "연결됨"),
        CompletedCheckItem(Icons.Outlined.CloudQueue, "서버", "연결됨"),
        CompletedCheckItem(Icons.Outlined.BatteryFull, "배터리", "충분"),
        CompletedCheckItem(Icons.Outlined.EnergySavingsLeaf, "절전모드", "해제됨"),
    )
}
