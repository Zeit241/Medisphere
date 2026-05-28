package com.example.kursovaya.model.api

import org.junit.Assert.*
import org.junit.Test

class LoginResponseTest {
    @Test
    fun `LoginResponse creation with all fields`() {
        val response =
            LoginResponse(
                token = "abc123token",
                email = "test@example.com",
                message = "Успешный вход",
            )

        assertEquals("abc123token", response.token)
        assertEquals("test@example.com", response.email)
        assertEquals("Успешный вход", response.message)
    }

    @Test
    fun `LoginResponse equality test`() {
        val response1 =
            LoginResponse(
                token = "token123",
                email = "test@example.com",
                message = "Success",
            )

        val response2 =
            LoginResponse(
                token = "token123",
                email = "test@example.com",
                message = "Success",
            )

        assertEquals(response1, response2)
        assertEquals(response1.hashCode(), response2.hashCode())
    }
}
