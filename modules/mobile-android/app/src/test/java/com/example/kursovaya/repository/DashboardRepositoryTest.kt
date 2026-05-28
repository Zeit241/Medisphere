package com.example.kursovaya.repository

import com.example.kursovaya.api.DashboardApi
import com.example.kursovaya.integration.RepositoryMockWebServerTestBase
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DashboardRepositoryTest : RepositoryMockWebServerTestBase() {
    private fun dashboardJson() =
        """
        {"topSpecializations":[{"id":1,"code":"THER","name":"Therapy","description":null,"doctorCount":2}],"topDoctors":[],"upcomingAppointments":[]}
        """.trimIndent()

    @Before
    fun clearCache() {
        DashboardRepository.clearCache()
    }

    private fun createRepository(): DashboardRepository {
        prepareRetrofit()
        val repository = DashboardRepository(context)
        bindApi<DashboardApi>(repository, "dashboardApi")
        return repository
    }

    @Test
    fun getDashboard_fetchesFromApiAndCaches() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setBody(dashboardJson())
                    .addHeader("Content-Type", "application/json"),
            )
            val repository = createRepository()

            val first = repository.getDashboard(forceRefresh = true)
            val second = repository.getDashboard()

            assertTrue(first.isSuccess)
            assertSame(first.getOrNull(), second.getOrNull())
            assertEquals(1, server.requestCount)
        }

    @Test
    fun getDashboard_forceRefreshBypassesCache() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setBody(dashboardJson())
                    .addHeader("Content-Type", "application/json"),
            )
            server.enqueue(
                MockResponse()
                    .setBody(dashboardJson())
                    .addHeader("Content-Type", "application/json"),
            )
            val repository = createRepository()

            repository.getDashboard(forceRefresh = true)
            repository.getDashboard(forceRefresh = true)

            assertEquals(2, server.requestCount)
        }

    @Test
    fun getDashboard_returnsFailureOnHttpError() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(500))

            val result = createRepository().getDashboard(forceRefresh = true)

            assertTrue(result.isFailure)
            assertEquals("Ошибка получения dashboard: 500", result.exceptionOrNull()?.message)
        }

    @Test
    fun hasCachedData_reflectsCacheState() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setBody(dashboardJson())
                    .addHeader("Content-Type", "application/json"),
            )
            val repository = createRepository()

            assertTrue(!repository.hasCachedData())
            repository.getDashboard(forceRefresh = true)
            assertTrue(repository.hasCachedData())
            assertEquals("Therapy", repository.getCachedDashboard()!!.topSpecializations.first().name)
        }
}
