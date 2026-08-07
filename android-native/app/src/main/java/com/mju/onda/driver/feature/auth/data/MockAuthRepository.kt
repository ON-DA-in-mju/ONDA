package com.mju.onda.driver.feature.auth.data

import com.mju.onda.driver.core.constants.AppStrings
import com.mju.onda.driver.data.mock.MockDriver
import com.mju.onda.driver.data.mock.MockUsers
import kotlinx.coroutines.delay

sealed class AuthResult {
    data class Success(val driver: MockDriver) : AuthResult()
    data class Failure(val message: String) : AuthResult()
}

class MockAuthRepository {
    suspend fun login(id: String, password: String): AuthResult {
        delay(700)

        val trimmedId = id.trim()
        val trimmedPassword = password.trim()

        if (trimmedId.isEmpty() || trimmedPassword.isEmpty()) {
            return AuthResult.Failure(AppStrings.LOGIN_EMPTY)
        }

        val driver = MockUsers.drivers.firstOrNull {
            it.id == trimmedId && it.password == trimmedPassword
        }

        return if (driver != null) {
            AuthResult.Success(driver)
        } else {
            AuthResult.Failure(AppStrings.LOGIN_FAILED)
        }
    }
}
