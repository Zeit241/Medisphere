package com.example.kursovaya.integration

import com.example.kursovaya.api.AppointmentApi
import com.example.kursovaya.model.api.BookAppointmentRequest
import com.example.kursovaya.model.api.CancelAppointmentRequest
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppointmentApiIntegrationTest : MockWebServerTestBase() {
    private val api: AppointmentApi by lazy { createApi() }

    private fun appointmentJson(id: Long = 1L) =
        """
        {"id":$id,"scheduleId":2,"doctorId":3,"patientId":4,"roomId":5,"startTime":"2026-03-01T10:00:00Z","endTime":"2026-03-01T10:30:00Z","isBooked":true,"status":"scheduled","source":"web","createdBy":4,"createdAt":"2026-02-01T00:00:00Z","updatedAt":"2026-02-01T00:00:00Z","cancelReason":null,"diagnosis":null}
        """.trimIndent()

    @Test
    fun getAppointmentsByPatientId_success() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setBody("[${appointmentJson()}]")
                    .addHeader("Content-Type", "application/json"),
            )
            val response = api.getAppointmentsByPatientId(4L)
            assertTrue(response.isSuccessful)
            assertEquals(1, response.body()!!.size)
        }

    @Test
    fun getAppointmentById_success() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setBody(appointmentJson(7L))
                    .addHeader("Content-Type", "application/json"),
            )
            val response = api.getAppointmentById(7L)
            assertTrue(response.isSuccessful)
            assertEquals(7L, response.body()!!.id)
        }

    @Test
    fun bookAppointment_success() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setBody(appointmentJson(9L))
                    .addHeader("Content-Type", "application/json"),
            )
            val response =
                api.bookAppointment(
                    BookAppointmentRequest(
                        appointmentId = 2L,
                        userId = 4L,
                        serviceId = 1L,
                    ),
                )
            assertTrue(response.isSuccessful)
            assertEquals(9L, response.body()!!.id)
        }

    @Test
    fun cancelAppointment_success() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setBody(appointmentJson(11L).replace("scheduled", "cancelled"))
                    .addHeader("Content-Type", "application/json"),
            )
            val response = api.cancelAppointment(11L, CancelAppointmentRequest(cancelReason = "busy"))
            assertTrue(response.isSuccessful)
            assertEquals("cancelled", response.body()!!.status)
        }
}
