package com.example.kursovaya.model

import org.junit.Assert.*
import org.junit.Test
import java.util.Date

class AppointmentTest {
    @Test
    fun `Appointment creation with all fields`() {
        val date = Date()
        val appointment =
            Appointment(
                id = "1",
                doctorId = "100",
                doctorName = "Доктор Смирнов",
                specialty = "Терапевт",
                date = date,
                time = "10:00",
                endTime = "10:30",
                roomCode = "101",
                roomName = "Кабинет 101",
                status = AppointmentStatus.UPCOMING,
                image = "image.jpg",
                phone = "+1234567890",
                email = "doctor@example.com",
                rating = 4.5f,
                reviewCount = 10,
                experienceYears = 10,
                bio = "Опытный врач",
                diagnosis = null,
                cancelReason = null,
            )

        assertEquals("1", appointment.id)
        assertEquals("100", appointment.doctorId)
        assertEquals("Доктор Смирнов", appointment.doctorName)
        assertEquals("Терапевт", appointment.specialty)
        assertEquals(date, appointment.date)
        assertEquals("10:00", appointment.time)
        assertEquals("10:30", appointment.endTime)
        assertEquals("101", appointment.roomCode)
        assertEquals(AppointmentStatus.UPCOMING, appointment.status)
        assertEquals(4.5f, appointment.rating, 0.01f)
        assertEquals(10, appointment.reviewCount)
    }

    @Test
    fun `Appointment with minimal fields`() {
        val date = Date()
        val appointment =
            Appointment(
                id = "2",
                doctorId = "200",
                doctorName = "Доктор Петров",
                specialty = "Хирург",
                date = date,
                time = "14:00",
                endTime = null,
                roomCode = "202",
                roomName = "Кабинет 202",
                status = AppointmentStatus.COMPLETED,
            )

        assertNull(appointment.endTime)
        assertNull(appointment.image)
        assertNull(appointment.phone)
        assertNull(appointment.email)
        assertEquals(0f, appointment.rating, 0.01f)
        assertEquals(0, appointment.reviewCount)
    }

    @Test
    fun `Appointment with cancelled status`() {
        val date = Date()
        val appointment =
            Appointment(
                id = "3",
                doctorId = "300",
                doctorName = "Доктор Иванов",
                specialty = "Кардиолог",
                date = date,
                time = "16:00",
                endTime = null,
                roomCode = "303",
                roomName = "Кабинет 303",
                status = AppointmentStatus.CANCELLED,
                cancelReason = "Пациент отменил",
            )

        assertEquals(AppointmentStatus.CANCELLED, appointment.status)
        assertEquals("Пациент отменил", appointment.cancelReason)
    }
}
