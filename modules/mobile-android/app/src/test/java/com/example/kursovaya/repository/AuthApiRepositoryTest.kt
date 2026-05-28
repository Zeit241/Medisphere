package com.example.kursovaya.repository

import com.example.kursovaya.api.AuthApi
import com.example.kursovaya.model.api.ApiResponse
import com.example.kursovaya.model.api.LoginRequest
import com.example.kursovaya.model.api.LoginResponse
import com.example.kursovaya.model.api.Patient
import com.example.kursovaya.model.api.RegisterWithPatientRequest
import com.example.kursovaya.model.api.RegisterWithPatientResponse
import com.example.kursovaya.model.api.User
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import retrofit2.Response

class AuthApiRepositoryTest {
    private val mockAuthApi: AuthApi = mock()
    private lateinit var authApiRepository: AuthApiRepository

    @Before
    fun setup() {
        authApiRepository = AuthApiRepository()
        val field = AuthApiRepository::class.java.getDeclaredField("authApi")
        field.isAccessible = true
        field.set(authApiRepository, mockAuthApi)
    }

    @Test
    fun login_returnsSuccessWhenApiSucceeds() =
        runTest {
            val loginResponse =
                LoginResponse(
                    token = "test-token",
                    email = "test@example.com",
                    message = "Успешный вход",
                )
            whenever(mockAuthApi.login(any<LoginRequest>())).thenReturn(
                Response.success(
                    ApiResponse(
                        success = true,
                        status = 200,
                        message = "Успешный вход",
                        data = loginResponse,
                    ),
                ),
            )

            val result = authApiRepository.login("test@example.com", "password123")

            assertTrue(result.isSuccess)
            assertEquals("test-token", result.getOrNull()?.token)
        }

    @Test
    fun login_returnsFailureWhenApiReportsError() =
        runTest {
            whenever(mockAuthApi.login(any<LoginRequest>())).thenReturn(
                Response.success(
                    ApiResponse<LoginResponse>(
                        success = false,
                        status = 401,
                        message = "Неверный email или пароль",
                        data = null,
                    ),
                ),
            )

            val result = authApiRepository.login("test@example.com", "wrongpassword")

            assertTrue(result.isFailure)
            assertEquals("Неверный email или пароль", result.exceptionOrNull()?.message)
        }

    @Test
    fun registerWithPatient_returnsSuccessWhenApiSucceeds() =
        runTest {
            val email = "newuser@example.com"
            val registerResponse =
                RegisterWithPatientResponse(
                    token = "new-token",
                    patient =
                        Patient(
                            id = 1L,
                            user =
                                User(
                                    1L,
                                    email,
                                    "+1234567890",
                                    "Ivan",
                                    "Ivanov",
                                    null,
                                    "2026-01-01T00:00:00Z",
                                    "2026-01-01T00:00:00Z",
                                    true,
                                ),
                            birthDate = "1990-01-01",
                            gender = 1,
                            insuranceNumber = null,
                            emergencyContact = null,
                            createdAt = "2026-01-01T00:00:00Z",
                            updatedAt = "2026-01-01T00:00:00Z",
                        ),
                    message = "Регистрация успешна",
                )
            whenever(mockAuthApi.registerWithPatient(any<RegisterWithPatientRequest>())).thenReturn(
                Response.success(
                    ApiResponse(
                        success = true,
                        status = 200,
                        message = "Регистрация успешна",
                        data = registerResponse,
                    ),
                ),
            )

            val result =
                authApiRepository.registerWithPatient(
                    email = email,
                    phone = "+1234567890",
                    password = "password123",
                    confirmPassword = "password123",
                    fio = "Иван Иванов",
                    birthDate = "1990-01-01",
                    gender = 1,
                )

            assertTrue(result.isSuccess)
            assertEquals("new-token", result.getOrNull()?.token)
        }
}
