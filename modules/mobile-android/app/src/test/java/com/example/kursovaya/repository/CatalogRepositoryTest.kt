package com.example.kursovaya.repository

import com.example.kursovaya.api.CatalogApi
import com.example.kursovaya.api.ClinicServicesApi
import com.example.kursovaya.integration.RepositoryMockWebServerTestBase
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogRepositoryTest : RepositoryMockWebServerTestBase() {
    private fun createRepository(): CatalogRepository {
        prepareRetrofit()
        val repository = CatalogRepository(context)
        bindApi<CatalogApi>(repository, "catalogApi")
        bindApi<ClinicServicesApi>(repository, "clinicServicesApi")
        return repository
    }

    @Test
    fun getAiReferenceCatalog_returnsCatalog() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setBody(
                        """
                        {"doctors":[{"id":1,"name":"Dr A","specialization":"Therapy"}],"services":[{"id":1,"name":"Consult"}]}
                        """.trimIndent(),
                    )
                    .addHeader("Content-Type", "application/json"),
            )

            val result = createRepository().getAiReferenceCatalog()

            assertTrue(result.isSuccess)
            assertEquals("Dr A", result.getOrNull()!!.doctors.first().name)
        }

    @Test
    fun getAiReferenceCatalog_returnsFailureOnHttpError() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(503))

            val result = createRepository().getAiReferenceCatalog()

            assertTrue(result.isFailure)
            assertEquals("Каталог: 503", result.exceptionOrNull()?.message)
        }

    @Test
    fun getServicesForDoctor_returnsServices() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setBody("""[{"id":1,"name":"Consult","price":1500.0,"durationMinutes":30}]""")
                    .addHeader("Content-Type", "application/json"),
            )

            val result = createRepository().getServicesForDoctor(3L)

            assertTrue(result.isSuccess)
            assertEquals("Consult", result.getOrNull()!!.first().name)
        }

    @Test
    fun getServicesForDoctor_returnsEmptyListWhenBodyNull() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(204))

            val result = createRepository().getServicesForDoctor(3L)

            assertTrue(result.isSuccess)
            assertTrue(result.getOrNull()!!.isEmpty())
        }
}
