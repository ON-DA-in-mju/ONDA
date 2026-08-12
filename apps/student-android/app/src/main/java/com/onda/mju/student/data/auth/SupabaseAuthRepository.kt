package com.onda.mju.student.data.auth

import com.onda.mju.student.data.remote.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email

/**
 * Supabase Auth login for student accounts.
 * Accepts a student ID or email; IDs are mapped to `@mju.ac.kr`.
 */
class SupabaseAuthRepository {

    suspend fun login(studentIdOrEmail: String, password: String): AuthResult {
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
                else -> throw e
            }
        }
    }
}
