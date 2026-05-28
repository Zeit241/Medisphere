package com.example.kursovaya.integration

import com.example.kursovaya.api.DoctorsApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DoctorsApiIntegrationTest : MockWebServerTestBase() {
    private val api: DoctorsApi by lazy { createApi() }

    @Test
    fun getDoctors_success() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setBody(
                        """
                        {"success":true,"status":200,"message":"ok","data":[{"id":1,"user":{"id":1,"email":"d@example.com","phone":"+7999","firstName":"Ann","lastName":"Doc","middleName":null,"createdAt":"2026-01-01T00:00:00Z","updatedAt":"2026-01-01T00:00:00Z","active":true},"displayName":"Ann Doc","bio":null,"experienceYears":5,"photo":null,"rating":4.5,"reviewCount":1,"specializations":[],"createdAt":"2026-01-01T00:00:00Z","updatedAt":"2026-01-01T00:00:00Z"}]}
                        """.trimIndent(),
                    )
                    .addHeader("Content-Type", "application/json"),
            )
            val response = api.getDoctors(limit = 10, offset = 0)
            assertTrue(response.isSuccessful)
            assertEquals(1, response.body()!!.data!!.size)
        }

    @Test
    fun getDoctorById_success() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setBody(
                        """
                        {"success":true,"status":200,"message":"ok","data":{"id":2,"user":{"id":2,"email":"d2@example.com","phone":"+7999","firstName":"Bob","lastName":"Doc","middleName":null,"createdAt":"2026-01-01T00:00:00Z","updatedAt":"2026-01-01T00:00:00Z","active":true},"displayName":"Bob Doc","bio":null,"experienceYears":3,"photo":null,"rating":4.0,"reviewCount":0,"specializations":[],"createdAt":"2026-01-01T00:00:00Z","updatedAt":"2026-01-01T00:00:00Z"}}
                        """.trimIndent(),
                    )
                    .addHeader("Content-Type", "application/json"),
            )
            val response = api.getDoctorById(2L)
            assertTrue(response.isSuccessful)
            assertEquals(2L, response.body()!!.data!!.id)
        }

    @Test
    fun getSpecializations_success() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setBody("""[{"id":1,"name":"Therapy","code":"THER"}]""")
                    .addHeader("Content-Type", "application/json"),
            )
            val response = api.getSpecializations()
            assertTrue(response.isSuccessful)
            assertEquals("Therapy", response.body()!!.first().name)
        }

    @Test
    fun getDoctorReviews_success() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setBody("""[{"id":1,"rating":5,"reviewText":"Great","createdAt":"2026-01-01T00:00:00Z","doctorId":1,"patientId":2}]""")
                    .addHeader("Content-Type", "application/json"),
            )
            val response = api.getDoctorReviews(1L)
            assertTrue(response.isSuccessful)
            assertEquals(5, response.body()!!.first().rating)
        }
}
