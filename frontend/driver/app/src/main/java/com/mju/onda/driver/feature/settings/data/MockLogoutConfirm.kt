package com.mju.onda.driver.feature.settings.data



object MockLogoutConfirm {

    const val SCREEN_TITLE = "로그아웃"

    const val HEADLINE = "로그아웃하시겠습니까?"

    const val SUBTITLE_AUTO_LOGIN_OFF =

        "현재 계정에서 로그아웃되며,\n다음 이용시 다시 로그인해야 합니다."

    const val SUBTITLE_AUTO_LOGIN_ON =

        "현재 계정에서 로그아웃됩니다.\n자동 로그인이 켜져 있어도 로그아웃 후에는 다시 로그인해야 합니다."



    const val LABEL_DRIVER = "기사명"

    const val LABEL_AUTO_LOGIN = "자동 로그인"

    const val AUTO_LOGIN_ON = "사용 중"

    const val AUTO_LOGIN_OFF = "사용 안 함"



    const val CONFIRM_LABEL = "로그아웃"

    const val CANCEL_LABEL = "취소"

    const val LOGOUT_TOAST = "로그아웃되었습니다."



    fun subtitle(autoLoginEnabled: Boolean): String =

        if (autoLoginEnabled) SUBTITLE_AUTO_LOGIN_ON else SUBTITLE_AUTO_LOGIN_OFF



    fun autoLoginLabel(autoLoginEnabled: Boolean): String =

        if (autoLoginEnabled) AUTO_LOGIN_ON else AUTO_LOGIN_OFF

}

