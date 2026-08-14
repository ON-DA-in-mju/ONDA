package com.mju.onda.driver.feature.auth.data

import com.mju.onda.driver.core.constants.AppStrings
import com.mju.onda.driver.core.supabase.SupabaseClient
import com.mju.onda.driver.data.mock.MockDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

sealed class AuthResult {
    data class Success(val driver: MockDriver) : AuthResult()
    data class Failure(val message: String) : AuthResult()
}

/**
 * Supabase Auth 로그인. ID는 `user01` 또는 `user01@mju.ac.kr` 형식.
 */
class AuthRepository {
    suspend fun login(id: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        val trimmedId = id.trim()
        val trimmedPassword = password.trim()

        if (trimmedId.isEmpty() || trimmedPassword.isEmpty()) {
            return@withContext AuthResult.Failure(AppStrings.LOGIN_EMPTY)
        }
        if (!SupabaseClient.isConfigured) {
            return@withContext AuthResult.Failure("Supabase 설정이 없습니다. local.properties를 확인하세요.")
        }

        val email = SupabaseClient.resolveEmail(trimmedId)
        val payload = JSONObject()
            .put("email", email)
            .put("password", trimmedPassword)
            .toString()

        val result = SupabaseClient.request(
            method = "POST",
            path = "/auth/v1/token?grant_type=password",
            jsonBody = payload,
            authed = false,
        )
        if (result.code !in 200..299) {
            return@withContext AuthResult.Failure(AppStrings.LOGIN_FAILED)
        }

        val root = JSONObject(result.body)
        val access = root.optString("access_token")
        val refresh = root.optString("refresh_token").ifBlank { null }
        val user = root.optJSONObject("user")
        val uuid = user?.optString("id").orEmpty()
        if (access.isBlank() || uuid.isBlank()) {
            return@withContext AuthResult.Failure(AppStrings.LOGIN_FAILED)
        }

        // 세션을 먼저 저장해야 이후 users 조회에 Bearer가 붙는다.
        SupabaseClient.saveSession(
            access = access,
            refresh = refresh,
            uuid = uuid,
            login = trimmedId.substringBefore("@"),
            name = trimmedId.substringBefore("@"),
            mail = email,
        )

        val profile = fetchProfile(uuid)
        val loginId = profile?.loginId ?: trimmedId.substringBefore("@")
        val name = profile?.name ?: loginId
        val org = profile?.organization ?: "명지 셔틀 운영팀"

        SupabaseClient.saveSession(
            access = access,
            refresh = refresh,
            uuid = uuid,
            login = loginId,
            name = name,
            mail = email,
        )

        AuthResult.Success(
            MockDriver(
                id = loginId,
                password = "",
                name = name,
                organization = org,
            ),
        )
    }

    private data class Profile(val loginId: String, val name: String, val organization: String)

    private fun fetchProfile(uuid: String): Profile? {
        val result = SupabaseClient.request(
            method = "GET",
            path = "/rest/v1/users?select=id,name,login_id,email,role&id=eq.$uuid",
            authed = true,
        )
        if (result.code !in 200..299) return null
        val arr = org.json.JSONArray(result.body)
        if (arr.length() == 0) return null
        val row = arr.getJSONObject(0)
        val loginId = row.optString("login_id").ifBlank {
            row.optString("email").substringBefore("@")
        }
        return Profile(
            loginId = loginId,
            name = row.optString("name").ifBlank { loginId },
            organization = "명지 셔틀 운영팀",
        )
    }
}
