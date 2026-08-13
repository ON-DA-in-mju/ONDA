package com.onda.mju.student.data.auth

import com.onda.mju.student.BuildConfig
import com.onda.mju.student.data.remote.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.exceptions.HttpRequestException
import kotlinx.coroutines.delay

/**
 * Supabase Auth login for student accounts.
 * Accepts a student ID or email; IDs are mapped to `@mju.ac.kr`.
 */
class SupabaseAuthRepository {

    fun hasActiveSession(): Boolean =
        runCatching {
            SupabaseClientProvider.client.auth.currentSessionOrNull() != null
        }.getOrDefault(false)

    /**
     * Auth 플러그인이 로컬 세션을 복원할 시간을 잠시 준 뒤 세션 유무를 확인한다.
     */
    suspend fun awaitActiveSession(maxWaitMillis: Long = 1_500L): Boolean {
        if (hasActiveSession()) return true
        val step = 100L
        var waited = 0L
        while (waited < maxWaitMillis) {
            delay(step)
            waited += step
            if (hasActiveSession()) return true
        }
        return false
    }

    suspend fun signOut() {
        runCatching {
            SupabaseClientProvider.client.auth.signOut()
        }
    }

    suspend fun login(studentIdOrEmail: String, password: String): AuthResult {
        if (BuildConfig.SUPABASE_URL.isBlank() || BuildConfig.SUPABASE_KEY.isBlank()) {
            return AuthResult.Failure(
                "Supabase 설정이 없습니다. frontend/student/local.properties 에 SUPABASE_URL, SUPABASE_KEY 를 넣고 다시 빌드하세요.",
            )
        }

        val trimmed = studentIdOrEmail.trim()
        val email = if (trimmed.contains("@")) {
            trimmed
        } else {
            "$trimmed@mju.ac.kr"
        }

        return try {
            SupabaseClientProvider.client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            AuthResult.Success
        } catch (e: AuthRestException) {
            when (e.errorCode) {
                AuthErrorCode.InvalidCredentials,
                AuthErrorCode.UserNotFound,
                AuthErrorCode.EmailAddressInvalid,
                -> AuthResult.InvalidCredentials
                else -> AuthResult.Failure(e.message ?: "로그인에 실패했습니다.")
            }
        } catch (e: HttpRequestException) {
            AuthResult.Failure("서버에 연결할 수 없습니다. 네트워크와 Supabase URL 설정을 확인해주세요.")
        } catch (e: Exception) {
            AuthResult.Failure(e.message ?: "로그인 중 오류가 발생했습니다.")
        }
    }
}
