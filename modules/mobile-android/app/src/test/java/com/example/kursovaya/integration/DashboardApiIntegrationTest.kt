package com.example.kursovaya.integration

import com.example.kursovaya.api.DashboardApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardApiIntegrationTest : MockWebServerTestBase() {
    private val api: DashboardApi by lazy { createApi() }

    @Test
    fun getDashboard_success() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setBody(
                        """
                        {"topSpecializations":[{"id":1,"code":"THER","name":"Therapy","description":null,"doctorCount":2}],"topDoctors":[],"upcomingAppointments":[]}
                        """.trimIndent(),
                    )
                    .addHeader("Content-Type", "application/json"),
            )
            val response = api.getDashboard()
            assertTrue(response.isSuccessful)
            assertEquals("Therapy", response.body()!!.topSpecializations.first().name)
        }

    @Test
    fun getDashboard_emptyPayload() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setBody("""{"topSpecializations":[],"topDoctors":[],"upcomingAppointments":[]}""")
                    .addHeader("Content-Type", "application/json"),
            )
            val response = api.getDashboard()
            assertTrue(response.isSuccessful)
            assertTrue(response.body()!!.topDoctors.isEmpty())
        }

    @Test
    fun getDashboard_serverError() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(500))
            val response = api.getDashboard()
            assertEquals(500, response.code())
        }

    @Test
    fun getDashboard_usesDashboardPath() =
        runTest {
            server.enqueue(MockResponse().setBody("""{"topSpecializations":[],"topDoctors":[],"upcomingAppointments":[]}"""))
            api.getDashboard()
            assertEquals("/api/dashboard", server.takeRequest().path)
        }
}
