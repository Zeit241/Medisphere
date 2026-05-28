package com.example.kursovaya.repository

import com.example.kursovaya.integration.RepositoryMockWebServerTestBase
import com.example.kursovaya.model.api.UpdatePatientRequest
import com.example.kursovaya.model.api.UpdateUserRequest
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class PatientRepositoryTest : RepositoryMockWebServerTestBase() {
    private fun sampleRequest() =
        UpdatePatientRequest(
            user =
                UpdateUserRequest(
                    email = "user@example.com",
                    phone = "+79990001122",
                    firstName = "Ivan",
                    lastName = "Petrov",
                    middleName = null,
                ),
            birthDate = "1990-01-01",
            gender = 1,
            insuranceNumber = null,
        )

    private fun createRepository(refreshUser: suspend () -> Result<com.example.kursovaya.model.api.User> = { Result.success(TestFixtures.user()) }): PatientRepository {
        prepareRetrofit()
        val repository = PatientRepository(context, refreshUserAfterUpdate = refreshUser)
        bindRetrofitForGetters()
        return repository
    }

    @Test
    fun updatePatient_returnsUpdatedInfoAndRefreshesUser() =
        runTest {
            val refreshCalls = AtomicInteger(0)
            server.enqueue(
                MockResponse()
                    .setBody(
                        """
                        {"success":true,"status":200,"message":"ok","data":{"id":4,"birthDate":"1990-01-01","gender":1,"insuranceNumber":null,"createdAt":"2026-01-01T00:00:00Z","updatedAt":"2026-01-02T00:00:00Z"}}
                        """.trimIndent(),
                    )
                    .addHeader("Content-Type", "application/json"),
            )

            val result =
                createRepository {
                    refreshCalls.incrementAndGet()
                    Result.success(TestFixtures.user())
                }.updatePatient(4L, sampleRequest())

            assertTrue(result.isSuccess)
            assertEquals(4L, result.getOrNull()!!.id)
            assertEquals(1, refreshCalls.get())
        }

    @Test
    fun updatePatient_returnsFailureWhenApiFails() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setResponseCode(400)
                    .setBody("""{"success":false,"status":400,"message":"validation error","data":null}""")
                    .addHeader("Content-Type", "application/json"),
            )

            val result = createRepository().updatePatient(4L, sampleRequest())

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("validation error") == true)
        }
}
