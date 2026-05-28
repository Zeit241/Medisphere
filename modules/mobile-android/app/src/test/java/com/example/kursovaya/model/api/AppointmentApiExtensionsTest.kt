package com.example.kursovaya.model.api

import com.example.kursovaya.model.AppointmentStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppointmentApiExtensionsTest {
    private fun sampleApi(
        status: String = "scheduled",
        serviceName: String? = "Consultation",
        servicePrice: Double? = 1500.0,
    ) = AppointmentApi(
        id = 1L,
        scheduleId = 2L,
        doctorId = 3L,
        patientId = 4L,
        roomId = 5L,
        startTime = "2026-03-01T10:00:00Z",
        endTime = "2026-03-01T10:30:00Z",
        isBooked = true,
        status = status,
        source = "web",
        createdBy = 4L,
        createdAt = "2026-02-01T00:00:00Z",
        updatedAt = "2026-02-01T00:00:00Z",
        cancelReason = null,
        diagnosis = null,
        service = AppointmentServiceApi(1L, serviceName, "CONS", servicePrice, 30),
    )

    @Test
    fun toAppointment_mapsScheduledToUpcoming() {
        val appointment =
            sampleApi("scheduled").toAppointment(
                doctorName = "Dr. A",
                doctorSpecialty = "Therapy",
                doctorPhone = null,
                doctorEmail = null,
                doctorImage = null,
            )
        assertEquals(AppointmentStatus.UPCOMING, appointment.status)
        assertEquals("Dr. A", appointment.doctorName)
    }

    @Test
    fun toAppointment_mapsCompletedStatus() {
        val appointment =
            sampleApi("completed").toAppointment(
                "Dr. B",
                "Surgery",
                null,
                null,
                null,
            )
        assertEquals(AppointmentStatus.COMPLETED, appointment.status)
    }

    @Test
    fun toAppointment_mapsCancelledStatus() {
        val appointment =
            sampleApi("cancelled").toAppointment(
                "Dr. C",
                "Therapy",
                null,
                null,
                null,
            )
        assertEquals(AppointmentStatus.CANCELLED, appointment.status)
    }

    @Test
    fun toAppointment_includesServiceNameAndPrice() {
        val appointment =
            sampleApi().toAppointment(
                "Dr. D",
                "Therapy",
                null,
                null,
                null,
            )
        assertEquals("Consultation", appointment.serviceName)
        assertEquals(true, appointment.servicePriceDisplay?.isNotBlank())
    }

    @Test
    fun toAppointment_formatsTimeFromIso() {
        val appointment =
            sampleApi().toAppointment(
                "Dr. E",
                "Therapy",
                null,
                null,
                null,
            )
        val expected =
            java.text.SimpleDateFormat("HH:mm", java.util.Locale("ru", "RU")).apply {
                timeZone = java.util.TimeZone.getDefault()
            }.format(
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }.parse("2026-03-01T10:00:00Z")!!,
            )
        assertEquals(expected, appointment.time)
    }

    @Test
    fun toAppointment_mapsCanceledAmericanSpelling() {
        val appointment =
            sampleApi("canceled").toAppointment(
                "Dr. G",
                "Therapy",
                null,
                null,
                null,
            )
        assertEquals(AppointmentStatus.CANCELLED, appointment.status)
    }

    @Test
    fun toAppointment_unknownStatusDefaultsToUpcoming() {
        val appointment =
            sampleApi("pending").toAppointment(
                "Dr. H",
                "Therapy",
                null,
                null,
                null,
            )
        assertEquals(AppointmentStatus.UPCOMING, appointment.status)
    }

    @Test
    fun toAppointment_formatsTimeFromUnparseableString() {
        val api = sampleApi().copy(startTime = "bad-time")
        val appointment =
            api.toAppointment(
                "Dr. I",
                "Therapy",
                null,
                null,
                null,
            )
        assertEquals("bad-time", appointment.time)
    }

    @Test
    fun toAppointment_nullServiceFields() {
        val appointment =
            sampleApi(serviceName = null, servicePrice = null).toAppointment(
                "Dr. F",
                "Therapy",
                null,
                null,
                null,
            )
        assertNull(appointment.serviceName)
        assertNull(appointment.servicePriceDisplay)
    }
}
