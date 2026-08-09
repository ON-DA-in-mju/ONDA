package com.mju.onda.driver.feature.backgroundguide.data

data class BackgroundGuideItem(
    val title: String,
    val description: String,
    val iconKind: BackgroundGuideIcon,
)

enum class BackgroundGuideIcon {
    ScreenLock,
    NoForceStop,
    Battery,
}

object MockBackgroundGuide {
    const val SCREEN_TITLE = "백그라운드 작동 안내"
    const val HEADLINE = "화면이 꺼져도\n위치 전송은 계속됩니다."
    const val SUBTITLE = "앱을 종료하거나 다른 앱을 사용해도\n운행 중에는 위치 전송이 유지됩니다."
    const val FOOTER_NOTICE = "문제가 발생하면 관리자에게 문의해 주세요."
    const val CONFIRM_LABEL = "확인"
    const val DONT_SHOW_LABEL = "다시 보지 않기"

    val items: List<BackgroundGuideItem> = listOf(
        BackgroundGuideItem(
            title = "화면 잠금 가능",
            description = "화면이 꺼져도 위치가 계속 전송됩니다.",
            iconKind = BackgroundGuideIcon.ScreenLock,
        ),
        BackgroundGuideItem(
            title = "앱 강제 종료 금지",
            description = "안정적인 전송을 위해 앱을 강제로 종료하지 마세요.",
            iconKind = BackgroundGuideIcon.NoForceStop,
        ),
        BackgroundGuideItem(
            title = "배터리 최적화 확인",
            description = "절전모드로 인해 전송이 중단되지 않도록 확인해 주세요.",
            iconKind = BackgroundGuideIcon.Battery,
        ),
    )
}
