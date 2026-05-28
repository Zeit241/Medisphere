package com.example.kursovaya.model.api

import org.junit.Assert.*
import org.junit.Test

class UserTest {
    @Test
    fun `User creation with all fields`() {
        val user =
            User(
                id = 1L,
                email = "test@example.com",
                phone = "+1234567890",
                firstName = "Иван",
                lastName = "Иванов",
                middleName = "Иванович",
                createdAt = "2024-01-01",
                updatedAt = "2024-01-02",
                active = true,
                patientId = 100L,
                doctorId = null,
                patient = null,
                doctor = null,
            )

        assertEquals(1L, user.id)
        assertEquals("test@example.com", user.email)
        assertEquals("+1234567890", user.phone)
        assertEquals("Иван", user.firstName)
        assertEquals("Иванов", user.lastName)
        assertEquals("Иванович", user.middleName)
        assertTrue(user.active)
        assertEquals(100L, user.patientId)
        assertNull(user.doctorId)
    }

    @Test
    fun `User creation with null middleName`() {
        val user =
            User(
                id = 2L,
                email = "test2@example.com",
                phone = "+9876543210",
                firstName = "Петр",
                lastName = "Петров",
                middleName = null,
                createdAt = "2024-01-01",
                updatedAt = "2024-01-02",
                active = false,
                patientId = null,
                doctorId = 200L,
                patient = null,
                doctor = null,
            )

        assertNull(user.middleName)
        assertFalse(user.active)
        assertNull(user.patientId)
        assertEquals(200L, user.doctorId)
    }

    @Test
    fun `User equality test`() {
        val user1 =
            User(
                id = 1L,
                email = "test@example.com",
                phone = "+1234567890",
                firstName = "Иван",
                lastName = "Иванов",
                middleName = "Иванович",
                createdAt = "2024-01-01",
                updatedAt = "2024-01-02",
                active = true,
                patientId = null,
                doctorId = null,
                patient = null,
                doctor = null,
            )

        val user2 =
            User(
                id = 1L,
                email = "test@example.com",
                phone = "+1234567890",
                firstName = "Иван",
                lastName = "Иванов",
                middleName = "Иванович",
                createdAt = "2024-01-01",
                updatedAt = "2024-01-02",
                active = true,
                patientId = null,
                doctorId = null,
                patient = null,
                doctor = null,
            )

        assertEquals(user1, user2)
        assertEquals(user1.hashCode(), user2.hashCode())
    }
}
