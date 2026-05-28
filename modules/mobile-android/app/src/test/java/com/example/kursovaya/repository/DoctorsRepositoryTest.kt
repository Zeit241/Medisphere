package com.example.kursovaya.repository

import com.example.kursovaya.api.DoctorsApi
import com.example.kursovaya.integration.RepositoryMockWebServerTestBase
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DoctorsRepositoryTest : RepositoryMockWebServerTestBase() {
    @Before
    fun clearCache() {
        DoctorsListCache.invalidateAll()
    }

    private fun doctorJson(id: Long = 1L) =
        """
        {"id":$id,"user":{"id":$id,"email":"d@example.com","phone":"+7999","firstName":"Ann","lastName":"Doc","middleName":null,"createdAt":"2026-01-01T00:00:00Z","updatedAt":"2026-01-01T00:00:00Z","active":true},"displayName":"Ann Doc","bio":null,"experienceYears":5,"photoUrl":null,"rating":4.5,"reviewCount":1,"specializations":[{"id":1,"code":"THER","name":"Therapy","description":null}],"createdAt":"2026-01-01T00:00:00Z","updatedAt":"2026-01-01T00:00:00Z"}
        """.trimIndent()

    private fun createRepository(): DoctorsRepository {
        prepareRetrofit()
        val repository = DoctorsRepository(context)
        bindApi<DoctorsApi>(repository, "doctorsApi")
        return repository
    }

    @Test
    fun getDoctors_returnsSuccessAndCachesPage() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setBody("""{"success":true,"status":200,"message":"ok","data":[${doctorJson()}]}""")
                    .addHeader("Content-Type", "application/json"),
            )
            val repository = createRepository()

            val first = repository.getDoctors(limit = 10, offset = 0)
            val second = repository.getDoctors(limit = 10, offset = 0)

            assertTrue(first.isSuccess)
            assertEquals(1, first.getOrNull()!!.size)
            assertEquals(first.getOrNull(), second.getOrNull())
            assertEquals(1, server.requestCount)
        }

    @Test
    fun getDoctors_returnsFailureWhenApiFails() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setBody("""{"success":false,"status":500,"message":"server error","data":null}""")
                    .addHeader("Content-Type", "application/json"),
            )

            val result = createRepository().getDoctors()

            assertTrue(result.isFailure)
            assertEquals("server error", result.exceptionOrNull()?.message)
        }

    @Test
    fun getDoctorById_returnsDoctor() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setBody("""{"success":true,"status":200,"message":"ok","data":${doctorJson(7)}}""")
                    .addHeader("Content-Type", "application/json"),
            )

            val result = createRepository().getDoctorById(7L)

            assertTrue(result.isSuccess)
            assertEquals(7L, result.getOrNull()!!.id)
        }

    @Test
    fun getSpecializations_returnsList() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setBody("""[{"id":1,"code":"THER","name":"Therapy","description":null}]""")
                    .addHeader("Content-Type", "application/json"),
            )

            val result = createRepository().getSpecializations()

            assertTrue(result.isSuccess)
            assertEquals("Therapy", result.getOrNull()!!.first().name)
        }

    @Test
    fun getDoctorReviews_returnsReviews() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setBody("""[{"id":1,"appointmentId":10,"doctorId":3,"patientId":4,"patientName":"Patient","rating":5,"reviewText":"Great","createdAt":"2026-01-01T00:00:00Z"}]""")
                    .addHeader("Content-Type", "application/json"),
            )

            val result = createRepository().getDoctorReviews(3L)

            assertTrue(result.isSuccess)
            assertEquals(5, result.getOrNull()!!.first().rating)
        }
}
