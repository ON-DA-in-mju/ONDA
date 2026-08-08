package com.mju.onda.driver.core



import com.mju.onda.driver.feature.auth.data.SessionStateHolder



/**

 * 테스트용 초기화: 현재 로그인한 계정의 데이터와 세션만 삭제한다.

 */

object DemoReset {

    fun resetCurrentUser() {

        val userId = SessionStateHolder.currentUserId

        UserScopedState.unbind()

        if (!userId.isNullOrBlank()) {

            UserScopedPrefs.clearAllForUser(userId)

        }

        SessionStateHolder.clear()

    }



    @Deprecated("Use resetCurrentUser()", ReplaceWith("resetCurrentUser()"))

    fun resetAll() = resetCurrentUser()

}

