package com.example.kursovaya.model.api

import org.junit.Assert.*
import org.junit.Test

class RegisterWithPatientRequestTest {
    @Test
    fun `RegisterWithPatientRequest creation with all fields`() {
        val emergencyContact =
            EmergencyContact(
                name = "Иван Иванов",
                phone = "+1234567890",
                relation = "Отец",
            )

        val request =
            RegisterWithPatientRequest(
                email = "test@example.com",
                phone = "+1234567890",
                password = "password123",
                confirmPassword = "password123",
                fio = "Петр Петров",
                birthDate = "1990-01-01",
                gender = 1,
                insuranceNumber = "123456789",
                emergencyContact = emergencyContact,
            )

        assertEquals("test@example.com", request.email)
        assertEquals("+1234567890", request.phone)
        assertEquals("password123", request.password)
        assertEquals("password123", request.confirmPassword)
        assertEquals("Петр Петров", request.fio)
        assertEquals("1990-01-01", request.birthDate)
        assertEquals(1, request.gender.toInt())
        assertEquals("123456789", request.insuranceNumber)
        assertNotNull(request.emergencyContact)
    }

    @Test
    fun `RegisterWithPatientRequest creation with optional fields as null`() {
        val request =
            RegisterWithPatientRequest(
                email = "test2@example.com",
                phone = "+9876543210",
                password = "password456",
                confirmPassword = "password456",
                fio = "Мария Иванова",
                birthDate = "1995-05-15",
                gender = 2,
                insuranceNumber = null,
                emergencyContact = null,
            )

        assertNull(request.insuranceNumber)
        assertNull(request.emergencyContact)
        assertEquals(2, request.gender.toInt())
    }

    @Test
    fun `RegisterWithPatientRequest equality test`() {
        val request1 =
            RegisterWithPatientRequest(
                email = "test@example.com",
                phone = "+1234567890",
                password = "password123",
                confirmPassword = "password123",
                fio = "Иван Иванов",
                birthDate = "1990-01-01",
                gender = 1,
            )

        val request2 =
            RegisterWithPatientRequest(
                email = "test@example.com",
                phone = "+1234567890",
                password = "password123",
                confirmPassword = "password123",
                fio = "Иван Иванов",
                birthDate = "1990-01-01",
                gender = 1,
            )

        assertEquals(request1, request2)
        assertEquals(request1.hashCode(), request2.hashCode())
    }

    @Test
    fun `EmergencyContact creation`() {
        val contact =
            EmergencyContact(
                name = "Иван Иванов",
                phone = "+1234567890",
                relation = "Отец",
            )

        assertEquals("Иван Иванов", contact.name)
        assertEquals("+1234567890", contact.phone)
        assertEquals("Отец", contact.relation)
    }
}
