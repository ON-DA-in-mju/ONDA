package com.mju.onda.driver.feature.settings.data

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CarCrash
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.ui.graphics.vector.ImageVector
import com.mju.onda.driver.R

data class StopReasonItem(
    val id: String,
    val label: String,
    val icon: ImageVector? = null,
    @DrawableRes val iconRes: Int? = null,
    val iconScale: Float = 1f,
)

object MockStopReasonSelect {
    const val SCREEN_TITLE = "중단 사유 선택"
    const val HEADLINE = "운행 중단 사유를 선택해 주세요."
    const val SUBTITLE = "가장 가까운 상황을 선택하면 관리자에게 전달됩니다."
    const val FOOTER_INFO = "선택 후 상세 내용을 입력할 수 있습니다."
    const val WARNING = "반드시 안전한 장소에 정차한 뒤 선택해 주세요."
    const val NEXT_LABEL = "다음"
    const val PREV_LABEL = "이전"
    const val NEXT_TOAST_PREFIX = "선택됨: "
    const val OTHER_ID = "other"
    const val OTHER_HINT = "상세 내용을 입력해 주세요. (10자 이내)"
    const val OTHER_MAX_LENGTH = 10

    val reasons: List<StopReasonItem> = listOf(
        StopReasonItem(id = "breakdown", label = "차량 고장", icon = Icons.Outlined.Build),
        StopReasonItem(id = "accident", label = "교통사고", icon = Icons.Outlined.CarCrash),
        StopReasonItem(id = "weather", label = "기상악화", icon = Icons.Outlined.Thunderstorm),
        StopReasonItem(
            id = "road_control",
            label = "도로 통제",
            iconRes = R.drawable.ic_road_control,
            iconScale = 0.85f,
        ),
        StopReasonItem(id = "passenger", label = "승객 안전 문제", icon = Icons.Outlined.Person),
        StopReasonItem(id = OTHER_ID, label = "기타", icon = Icons.Outlined.MoreHoriz),
    )
}
