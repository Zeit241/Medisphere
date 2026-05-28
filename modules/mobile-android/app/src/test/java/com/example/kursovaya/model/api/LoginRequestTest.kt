package com.example.kursovaya.model.api

import org.junit.Assert.*
import org.junit.Test

class LoginRequestTest {
    @Test
    fun `LoginRequest creation with email and password`() {
        val request =
            LoginRequest(
                email = "test@example.com",
                password = "password123",
            )

        assertEquals("test@example.com", request.email)
        assertEquals("password123", request.password)
    }

    @Test
    fun `LoginRequest equality test`() {
        val request1 =
            LoginRequest(
                email = "test@example.com",
                password = "password123",
            )

        val request2 =
            LoginRequest(
                email = "test@example.com",
                password = "password123",
            )

        assertEquals(request1, request2)
        assertEquals(request1.hashCode(), request2.hashCode())
    }

    @Test
    fun `LoginRequest toString test`() {
        val request =
            LoginRequest(
                email = "test@example.com",
                password = "password123",
            )

        val toString = request.toString()
        assertTrue(toString.contains("test@example.com"))
    }
}
