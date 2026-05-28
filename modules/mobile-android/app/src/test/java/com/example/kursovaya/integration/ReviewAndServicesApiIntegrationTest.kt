package com.example.kursovaya.integration

import com.example.kursovaya.api.ClinicServicesApi
import com.example.kursovaya.api.ReviewApi
import com.example.kursovaya.model.api.AppointmentRef
import com.example.kursovaya.model.api.CreateReviewRequest
import com.example.kursovaya.model.api.DoctorRef
import com.example.kursovaya.model.api.PatientRef
import com.example.kursovaya.model.api.UpdateReviewRequest
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewAndServicesApiIntegrationTest : MockWebServerTestBase() {
    private val reviewApi: ReviewApi by lazy { createApi() }
    private val servicesApi: ClinicServicesApi by lazy { createApi() }

    private fun reviewJson(id: Long = 1L) =
        """
        {"id":$id,"appointmentId":10,"doctorId":3,"patientId":4,"patientName":"Patient","rating":5,"reviewText":"Great","createdAt":"2026-01-01T00:00:00Z"}
        """.trimIndent()

    @Test
    fun createReview_success() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setBody("""{"success":true,"status":200,"message":"ok","data":${reviewJson()}}""")
                    .addHeader("Content-Type", "application/json"),
            )
            val response =
                reviewApi.createReview(
                    CreateReviewRequest(
                        appointment = AppointmentRef(10L),
                        doctor = DoctorRef(3L),
                        patient = PatientRef(4L),
                        rating = 5,
                        reviewText = "Great",
                    ),
                )
            assertTrue(response.isSuccessful)
            assertEquals(5, response.body()!!.data!!.rating)
        }

    @Test
    fun getReviewByAppointmentId_success() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setBody(reviewJson(2L))
                    .addHeader("Content-Type", "application/json"),
            )
            val response = reviewApi.getReviewByAppointmentId(10L)
            assertTrue(response.isSuccessful)
            assertEquals(2L, response.body()!!.id)
        }

    @Test
    fun updateReview_success() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setBody("""{"success":true,"status":200,"message":"ok","data":${reviewJson(3L).replace("Great", "Updated")}}""")
                    .addHeader("Content-Type", "application/json"),
            )
            val response = reviewApi.updateReview(3L, UpdateReviewRequest(rating = 4, reviewText = "Updated"))
            assertTrue(response.isSuccessful)
            assertEquals("Updated", response.body()!!.data!!.reviewText)
        }

    @Test
    fun getServicesForDoctor_success() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setBody("""[{"id":1,"name":"Consultation","price":1500,"durationMinutes":30}]""")
                    .addHeader("Content-Type", "application/json"),
            )
            val response = servicesApi.getServicesForDoctor(3L)
            assertTrue(response.isSuccessful)
            assertEquals("Consultation", response.body()!!.first().name)
        }
}
