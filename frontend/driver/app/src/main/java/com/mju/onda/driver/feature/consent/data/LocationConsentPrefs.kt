package com.mju.onda.driver.feature.consent.data

import android.content.SharedPreferences
import com.mju.onda.driver.core.UserScopedPrefs
import com.mju.onda.driver.feature.auth.data.SessionStateHolder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 위치정보 이용 동의(앱 내 동의) 상태. 계정별 저장.
 * OS 위치 권한과는 별개이다.
 */
object LocationConsentPrefs {

    private const val PREFS = "onda_location_consent"
    private const val KEY_CONSENTED = "consented"
    private const val KEY_CONSENTED_AT = "consented_at_millis"

    private var prefs: SharedPreferences? = null

    @Volatile
    var isConsented: Boolean = false
        private set

    @Volatile
    var consentedAtMillis: Long = 0L
        private set

    fun bindUser() {
        prefs = UserScopedPrefs.get(PREFS)
        // 키가 없을 때만 온보딩 완료 계정을 동의로 이관 (철회 후 재로그인 시 덮어쓰지 않음)
        if (prefs?.contains(KEY_CONSENTED) != true) {
            if (SessionStateHolder.onboardingDone) {
                isConsented = true
                consentedAtMillis = 0L
                prefs?.edit()?.putBoolean(KEY_CONSENTED, true)?.apply()
            } else {
                isConsented = false
                consentedAtMillis = 0L
            }
            return
        }
        isConsented = prefs?.getBoolean(KEY_CONSENTED, false) == true
        consentedAtMillis = prefs?.getLong(KEY_CONSENTED_AT, 0L) ?: 0L
    }

    fun unbindUser() {
        isConsented = false
        consentedAtMillis = 0L
        prefs = null
    }

    fun markConsented(atMillis: Long = System.currentTimeMillis()) {
        isConsented = true
        consentedAtMillis = atMillis
        prefs?.edit()
            ?.putBoolean(KEY_CONSENTED, true)
            ?.putLong(KEY_CONSENTED_AT, atMillis)
            ?.commit()
    }

    /** 설정에서 동의 철회 */
    fun revokeConsent() {
        isConsented = false
        consentedAtMillis = 0L
        prefs?.edit()
            ?.putBoolean(KEY_CONSENTED, false)
            ?.putLong(KEY_CONSENTED_AT, 0L)
            ?.commit()
    }

    fun clear() {
        isConsented = false
        consentedAtMillis = 0L
        prefs?.edit()?.clear()?.apply()
    }

    fun formatConsentedAt(): String {
        if (!isConsented) return "미동의"
        if (consentedAtMillis <= 0L) return "기록 없음"
        return SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA)
            .format(Date(consentedAtMillis))
    }

    fun statusBadge(): String = if (isConsented) "동의 중" else "미동의"
}
