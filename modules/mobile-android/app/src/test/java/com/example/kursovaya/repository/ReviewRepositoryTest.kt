package com.example.kursovaya.repository

import com.example.kursovaya.integration.RepositoryMockWebServerTestBase
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewRepositoryTest : RepositoryMockWebServerTestBase() {
    private fun createRepository(): ReviewRepository {
        prepareRetrofit()
        val repository = ReviewRepository(context)
        bindRetrofitForGetters()
        return repository
    }

    @Test
    fun createReview_returnsCreatedReview() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setBody(
                        """
                        {"success":true,"status":200,"message":"ok","data":{"id":1,"appointmentId":10,"doctorId":3,"patientId":4,"patientName":"Patient","rating":5,"reviewText":"Great","createdAt":"2026-01-01T00:00:00Z"}}
                        """.trimIndent(),
                    )
                    .addHeader("Content-Type", "application/json"),
            )

            val result = createRepository().createReview(10L, 3L, 4L, 5, "Great")

            assertTrue(result.isSuccess)
            assertEquals(5, result.getOrNull()!!.rating)
        }

    @Test
    fun getReviewByAppointmentId_returnsNullOn404() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(404))

            val result = createRepository().getReviewByAppointmentId(99L)

            assertTrue(result.isSuccess)
            assertNull(result.getOrNull())
        }

    @Test
    fun getReviewByAppointmentId_returnsReview() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setBody(
                        """
                        {"id":2,"appointmentId":11,"doctorId":3,"patientId":4,"patientName":null,"rating":4,"reviewText":"Good","createdAt":"2026-01-01T00:00:00Z"}
                        """.trimIndent(),
                    )
                    .addHeader("Content-Type", "application/json"),
            )

            val result = createRepository().getReviewByAppointmentId(11L)

            assertTrue(result.isSuccess)
            assertEquals(4, result.getOrNull()!!.rating)
        }

    @Test
    fun updateReview_returnsUpdatedReview() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setBody(
                        """
                        {"success":true,"status":200,"message":"ok","data":{"id":3,"appointmentId":12,"doctorId":3,"patientId":4,"patientName":null,"rating":3,"reviewText":"Updated","createdAt":"2026-01-02T00:00:00Z"}}
                        """.trimIndent(),
                    )
                    .addHeader("Content-Type", "application/json"),
            )

            val result = createRepository().updateReview(3L, 3, "Updated")

            assertTrue(result.isSuccess)
            assertEquals("Updated", result.getOrNull()!!.reviewText)
        }

    @Test
    fun createReview_returnsFailureWhenApiReportsError() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setResponseCode(400)
                    .setBody("""{"success":false,"status":400,"message":"bad review","data":null}""")
                    .addHeader("Content-Type", "application/json"),
            )

            val result = createRepository().createReview(10L, 3L, 4L, 0, null)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("bad review") == true)
        }
}
