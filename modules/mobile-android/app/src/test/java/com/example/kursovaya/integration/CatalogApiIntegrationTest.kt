package com.example.kursovaya.integration

import com.example.kursovaya.api.CatalogApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogApiIntegrationTest : MockWebServerTestBase() {
    private val api: CatalogApi by lazy { createApi() }

    @Test
    fun getAiReference_success() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setBody(
                        """
                        {"doctors":[{"id":1,"name":"Ann Doc","specialization":"Therapy"}],"services":[{"id":2,"name":"Consultation"}]}
                        """.trimIndent(),
                    )
                    .addHeader("Content-Type", "application/json"),
            )
            val response = api.getAiReference()
            assertTrue(response.isSuccessful)
            assertEquals(1, response.body()!!.doctors.size)
            assertEquals("Consultation", response.body()!!.services.first().name)
        }

    @Test
    fun getAiReference_emptyLists() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setBody("""{"doctors":[],"services":[]}""")
                    .addHeader("Content-Type", "application/json"),
            )
            val response = api.getAiReference()
            assertTrue(response.isSuccessful)
            assertTrue(response.body()!!.doctors.isEmpty())
        }

    @Test
    fun getAiReference_notFound() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(404))
            val response = api.getAiReference()
            assertEquals(404, response.code())
        }

    @Test
    fun getAiReference_hitsCorrectPath() =
        runTest {
            server.enqueue(MockResponse().setBody("""{"doctors":[],"services":[]}"""))
            api.getAiReference()
            assertTrue(server.takeRequest().path!!.contains("/api/catalog/ai-reference"))
        }
}
