package com.example.kursovaya.model

import org.junit.Assert.*
import org.junit.Test

class AppointmentStatusTest {
    @Test
    fun `AppointmentStatus enum values exist`() {
        // Проверяем, что все ожидаемые значения существуют
        assertNotNull(AppointmentStatus.UPCOMING)
        assertNotNull(AppointmentStatus.COMPLETED)
        assertNotNull(AppointmentStatus.CANCELLED)
    }

    @Test
    fun `AppointmentStatus value property`() {
        // Проверяем, что у каждого статуса есть значение
        assertEquals("Предстоящие", AppointmentStatus.UPCOMING.value)
        assertEquals("Завершенные", AppointmentStatus.COMPLETED.value)
        assertEquals("Отмененные", AppointmentStatus.CANCELLED.value)
    }

    @Test
    fun `AppointmentStatus enum comparison`() {
        val status1 = AppointmentStatus.UPCOMING
        val status2 = AppointmentStatus.UPCOMING
        val status3 = AppointmentStatus.COMPLETED

        assertEquals(status1, status2)
        assertNotEquals(status1, status3)
    }
}
