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

    fun currentEmail(): String? =
        runCatching {
            SupabaseClientProvider.client.auth.currentUserOrNull()?.email
        }.getOrNull()

    /**
     * Auth 플러그인이 로컬 세션을 복원할 시간을 잠시 준 뒤 세션 유무를 확인한다.
     */
    suspend fun awaitActiveSession(maxWaitMillis: Long = 450L): Boolean {
        if (hasActiveSession()) return true
        val step = 40L
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

    /**
     * 현재 비밀번호 확인 후 Supabase Auth(DB auth.users) 비밀번호를 변경한다.
     * 앱/웹 로그인에 쓰는 해시는 Auth 쪽에만 저장되며, public.users 에는 비밀번호 컬럼이 없다.
     */
    suspend fun changePassword(currentPassword: String, newPassword: String): AuthResult {
        val email = currentEmail()?.trim().orEmpty()
        if (email.isBlank()) {
            return AuthResult.Failure("로그인 정보를 확인할 수 없습니다. 다시 로그인해 주세요.")
        }
        if (newPassword.length < 6) {
            return AuthResult.Failure("새 비밀번호는 6자 이상이어야 합니다.")
        }
        if (currentPassword == newPassword) {
            return AuthResult.Failure("현재 비밀번호와 다른 비밀번호를 입력해 주세요.")
        }

        return try {
            // 1) 현재 비밀번호로 Auth 재인증
            SupabaseClientProvider.client.auth.signInWith(Email) {
                this.email = email
                this.password = currentPassword
            }
            // 2) Auth DB(auth.users) encrypted_password 갱신
            SupabaseClientProvider.client.auth.updateUser {
                password = newPassword
            }
            android.util.Log.d("ONDA_AUTH", "password updated in Supabase Auth for $email")
            AuthResult.Success
        } catch (e: AuthRestException) {
            when (e.errorCode) {
                AuthErrorCode.InvalidCredentials,
                AuthErrorCode.UserNotFound,
                -> AuthResult.Failure("현재 비밀번호가 올바르지 않습니다.")
                else -> AuthResult.Failure(e.message ?: "비밀번호 변경에 실패했습니다.")
            }
        } catch (e: HttpRequestException) {
            AuthResult.Failure("서버에 연결할 수 없습니다. 네트워크를 확인해주세요.")
        } catch (e: Exception) {
            AuthResult.Failure(e.message ?: "비밀번호 변경 중 오류가 발생했습니다.")
        }
    }

    suspend fun login(studentIdOrEmail: String, password: String): AuthResult {
        if (BuildConfig.SUPABASE_URL.isBlank() || BuildConfig.SUPABASE_KEY.isBlank()) {
            return AuthResult.Failure(
                "Supabase 설정이 없습니다. apps/student-android/local.properties 에 SUPABASE_URL, SUPABASE_KEY 를 넣고 다시 빌드하세요.",
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
