package com.mju.onda.driver.core.supabase

import android.content.Context
import android.content.SharedPreferences
import com.mju.onda.driver.BuildConfig
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Supabase Auth + REST (실기기/다운로드 APK용).
 * Vite mock 서버에 의존하지 않는다.
 */
object SupabaseClient {
    private const val PREFS = "onda_supabase"
    private const val KEY_ACCESS = "access_token"
    private const val KEY_REFRESH = "refresh_token"
    private const val KEY_USER_UUID = "user_uuid"
    private const val KEY_LOGIN_ID = "login_id"
    private const val KEY_NAME = "display_name"
    private const val KEY_EMAIL = "email"

    private var prefs: SharedPreferences? = null

    val url: String
        get() = BuildConfig.SUPABASE_URL.trimEnd('/')

    val anonKey: String
        get() = BuildConfig.SUPABASE_ANON_KEY

    val isConfigured: Boolean
        get() = url.isNotBlank() &&
            anonKey.isNotBlank() &&
            !url.contains("YOUR_") &&
            anonKey != "YOUR_ANON_KEY"

    @Volatile
    var accessToken: String? = null
        private set

    @Volatile
    var userUuid: String? = null
        private set

    @Volatile
    var loginId: String? = null
        private set

    @Volatile
    var displayName: String? = null
        private set

    @Volatile
    var email: String? = null
        private set

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        accessToken = prefs?.getString(KEY_ACCESS, null)
        userUuid = prefs?.getString(KEY_USER_UUID, null)
        loginId = prefs?.getString(KEY_LOGIN_ID, null)
        displayName = prefs?.getString(KEY_NAME, null)
        email = prefs?.getString(KEY_EMAIL, null)
    }

    fun clearSession() {
        accessToken = null
        userUuid = null
        loginId = null
        displayName = null
        email = null
        prefs?.edit()?.clear()?.apply()
    }

    fun saveSession(
        access: String,
        refresh: String?,
        uuid: String,
        login: String,
        name: String,
        mail: String,
    ) {
        accessToken = access
        userUuid = uuid
        loginId = login
        displayName = name
        email = mail
        prefs?.edit()
            ?.putString(KEY_ACCESS, access)
            ?.putString(KEY_REFRESH, refresh)
            ?.putString(KEY_USER_UUID, uuid)
            ?.putString(KEY_LOGIN_ID, login)
            ?.putString(KEY_NAME, name)
            ?.putString(KEY_EMAIL, mail)
            ?.apply()
    }

    /** access token 만료 시 refresh_token으로 재발급 */
    fun refreshSession(): Boolean {
        val refresh = prefs?.getString(KEY_REFRESH, null)
        if (refresh.isNullOrBlank()) return false
        val payload = JSONObject().put("refresh_token", refresh).toString()
        val result = requestOnce(
            method = "POST",
            path = "/auth/v1/token?grant_type=refresh_token",
            jsonBody = payload,
            authed = false,
            prefer = null,
        )
        if (result.code !in 200..299) return false
        return runCatching {
            val root = JSONObject(result.body)
            val access = root.optString("access_token")
            if (access.isBlank()) return false
            val newRefresh = root.optString("refresh_token").ifBlank { refresh }
            accessToken = access
            prefs?.edit()
                ?.putString(KEY_ACCESS, access)
                ?.putString(KEY_REFRESH, newRefresh)
                ?.apply()
            true
        }.getOrDefault(false)
    }

    fun resolveEmail(idOrEmail: String): String {
        val trimmed = idOrEmail.trim()
        return if (trimmed.contains("@")) trimmed else "$trimmed@mju.ac.kr"
    }

    data class HttpResult(val code: Int, val body: String)

    fun request(
        method: String,
        path: String,
        jsonBody: String? = null,
        authed: Boolean = true,
        /** POST/PATCH Prefer 헤더. null이면 return=representation */
        prefer: String? = null,
    ): HttpResult {
        val first = requestOnce(method, path, jsonBody, authed, prefer)
        if (first.code != 401 || !authed) return first
        if (!refreshSession()) return first
        return requestOnce(method, path, jsonBody, authed, prefer)
    }

    private fun requestOnce(
        method: String,
        path: String,
        jsonBody: String?,
        authed: Boolean,
        prefer: String?,
    ): HttpResult {
        val conn = (URL("$url$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 12_000
            readTimeout = 12_000
            setRequestProperty("apikey", anonKey)
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            if (method == "PATCH" || method == "POST") {
                setRequestProperty("Prefer", prefer ?: "return=representation")
            }
            if (authed) {
                val token = accessToken
                if (!token.isNullOrBlank()) {
                    setRequestProperty("Authorization", "Bearer $token")
                } else {
                    setRequestProperty("Authorization", "Bearer $anonKey")
                }
            } else {
                setRequestProperty("Authorization", "Bearer $anonKey")
            }
            if (jsonBody != null) {
                doOutput = true
            }
        }
        try {
            if (jsonBody != null) {
                val bytes = jsonBody.toByteArray(Charsets.UTF_8)
                conn.setFixedLengthStreamingMode(bytes.size)
                conn.outputStream.use { it.write(bytes) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.let { BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { r -> r.readText() } }.orEmpty()
            return HttpResult(code, body)
        } finally {
            conn.disconnect()
        }
    }

    fun parseJsonObject(body: String): JSONObject = JSONObject(body)
}
