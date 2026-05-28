package com.example.kursovaya.integration

import com.example.kursovaya.api.AuthApi
import com.example.kursovaya.model.api.LoginRequest
import com.example.kursovaya.model.api.RegisterWithPatientRequest
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthApiIntegrationTest : MockWebServerTestBase() {
    private val api: AuthApi by lazy { createApi() }

    @Test
    fun login_success() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setBody(
                        """
                        {"success":true,"status":200,"message":"ok","data":{"token":"jwt","email":"a@example.com","message":"ok"}}
                        """.trimIndent(),
                    )
                    .addHeader("Content-Type", "application/json"),
            )
            val response = api.login(LoginRequest("a@example.com", "secret"))
            assertTrue(response.isSuccessful)
            assertEquals("jwt", response.body()!!.data!!.token)
        }

    @Test
    fun login_unauthorized() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setResponseCode(401)
                    .setBody("""{"success":false,"status":401,"message":"bad","data":null}""")
                    .addHeader("Content-Type", "application/json"),
            )
            val response = api.login(LoginRequest("a@example.com", "wrong"))
            assertEquals(401, response.code())
        }

    @Test
    fun registerWithPatient_success() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setBody(
                        """
                        {"success":true,"status":200,"message":"ok","data":{"token":"new-jwt","email":"n@example.com","message":"registered"}}
                        """.trimIndent(),
                    )
                    .addHeader("Content-Type", "application/json"),
            )
            val response =
                api.registerWithPatient(
                    RegisterWithPatientRequest(
                        email = "n@example.com",
                        phone = "+7999",
                        password = "secret",
                        confirmPassword = "secret",
                        fio = "Ivan Petrov",
                        birthDate = "1990-01-01",
                        gender = 1.toShort(),
                    ),
                )
            assertTrue(response.isSuccessful)
            assertNotNull(response.body()?.data?.token)
        }

    @Test
    fun login_sendsJsonBody() =
        runTest {
            server.enqueue(
                MockResponse().setBody(
                    """{"success":true,"status":200,"message":"ok","data":{"token":"t","email":"a@example.com","message":"ok"}}""",
                ),
            )
            api.login(LoginRequest("a@example.com", "secret"))
            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertTrue(request.path!!.contains("/api/auth/login"))
            assertTrue(request.body.readUtf8().contains("a@example.com"))
        }
}
