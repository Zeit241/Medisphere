package com.example.kursovaya.repository

import com.example.kursovaya.api.AppointmentApi
import com.example.kursovaya.api.DoctorsApi
import com.example.kursovaya.integration.RepositoryMockWebServerTestBase
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppointmentRepositoryTest : RepositoryMockWebServerTestBase() {
    private fun appointmentJson(id: Long = 1L) =
        """
        {"id":$id,"scheduleId":2,"doctorId":3,"patientId":4,"roomId":5,"startTime":"2026-03-01T10:00:00Z","endTime":"2026-03-01T10:30:00Z","isBooked":true,"status":"scheduled","source":"web","createdBy":4,"createdAt":"2026-02-01T00:00:00Z","updatedAt":"2026-02-01T00:00:00Z","cancelReason":null,"diagnosis":null,"service":null}
        """.trimIndent()

    private fun doctorJson() =
        """
        {"id":3,"user":{"id":3,"email":"d@example.com","phone":"+7999","firstName":"Ivan","lastName":"Petrov","middleName":null,"createdAt":"2026-01-01T00:00:00Z","updatedAt":"2026-01-01T00:00:00Z","active":true},"displayName":"Ivan Petrov","bio":null,"experienceYears":5,"photoUrl":null,"rating":4.5,"reviewCount":1,"specializations":[{"id":1,"code":"THER","name":"Therapy","description":null}],"createdAt":"2026-01-01T00:00:00Z","updatedAt":"2026-01-01T00:00:00Z"}
        """.trimIndent()

    private fun createRepository(): AppointmentRepository {
        prepareRetrofit()
        val repository = AppointmentRepository(context)
        bindApi<AppointmentApi>(repository, "appointmentApi")
        val doctorsRepository = DoctorsRepository(context)
        bindApi<DoctorsApi>(doctorsRepository, "doctorsApi")
        RepositoryTestHelper.setField(repository, "doctorsRepository", doctorsRepository)
        return repository
    }

    @Test
    fun getAppointmentsByPatientId_returnsList() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setBody("[${appointmentJson()}]")
                    .addHeader("Content-Type", "application/json"),
            )

            val result = createRepository().getAppointmentsByPatientId(4L)

            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrNull()!!.size)
        }

    @Test
    fun getAppointmentsByPatientId_returnsFailureOnHttpError() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(500))

            val result = createRepository().getAppointmentsByPatientId(4L)

            assertTrue(result.isFailure)
        }

    @Test
    fun getAppointmentsForPatient_mapsDoctorAndAppointment() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setBody("[${appointmentJson(5)}]")
                    .addHeader("Content-Type", "application/json"),
            )
            server.enqueue(
                MockResponse()
                    .setBody("""{"success":true,"status":200,"message":"ok","data":${doctorJson()}}""")
                    .addHeader("Content-Type", "application/json"),
            )

            val result = createRepository().getAppointmentsForPatient(4L)

            assertTrue(result.isSuccess)
            val appointment = result.getOrNull()!!.first()
            assertEquals("Petrov Ivan", appointment.doctorName)
            assertEquals("Therapy", appointment.specialty)
        }

    @Test
    fun bookAppointment_returnsBookedSlot() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setBody(appointmentJson(9))
                    .addHeader("Content-Type", "application/json"),
            )

            val result = createRepository().bookAppointment(2L, 4L, 1L)

            assertTrue(result.isSuccess)
            assertEquals(9L, result.getOrNull()!!.id)
        }

    @Test
    fun cancelAppointment_returnsCancelledSlot() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setBody(appointmentJson(11).replace("scheduled", "cancelled"))
                    .addHeader("Content-Type", "application/json"),
            )

            val result = createRepository().cancelAppointment(11L, "busy")

            assertTrue(result.isSuccess)
            assertEquals("cancelled", result.getOrNull()!!.status)
        }

    @Test
    fun getAvailableAppointments_returnsSlots() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setBody("[${appointmentJson(20)}]")
                    .addHeader("Content-Type", "application/json"),
            )

            val result = createRepository().getAvailableAppointments(3L, "2026-03-01", 1L)

            assertTrue(result.isSuccess)
            assertEquals(20L, result.getOrNull()!!.first().id)
        }
}
