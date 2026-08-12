package com.onda.mju.student.data.auth

import kotlinx.coroutines.delay

/**
 * Temporary mock authentication.
 * Replace this class with a Supabase Auth implementation later.
 */
class MockAuthRepository {

    suspend fun login(studentIdOrEmail: String, password: String): AuthResult {
        // Keep the delay short so login tests stay responsive.
        delay(350)

        val id = studentIdOrEmail.trim()
        val pw = password

        return if (id == MOCK_STUDENT_ID && pw == MOCK_PASSWORD) {
            AuthResult.Success
        } else {
            AuthResult.InvalidCredentials
        }
    }

    companion object {
        const val MOCK_STUDENT_ID = "60201234"
        const val MOCK_PASSWORD = "onda1234"
    }
}

sealed interface AuthResult {
    data object Success : AuthResult
    data object InvalidCredentials : AuthResult
}
