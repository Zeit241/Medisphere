package com.example.kursovaya.model.api

import org.junit.Assert.*
import org.junit.Test

class ApiResponseTest {
    @Test
    fun `isSuccessful returns true when success is true`() {
        val response =
            ApiResponse(
                success = true,
                status = null,
                message = "Success",
                data = "test",
            )
        assertTrue(response.isSuccessful())
    }

    @Test
    fun `isSuccessful returns true when status is 200`() {
        val response =
            ApiResponse(
                success = null,
                status = 200,
                message = "Success",
                data = "test",
            )
        assertTrue(response.isSuccessful())
    }

    @Test
    fun `isSuccessful returns false when success is false and status is not 200`() {
        val response =
            ApiResponse(
                success = false,
                status = 400,
                message = "Error",
                data = null,
            )
        assertFalse(response.isSuccessful())
    }

    @Test
    fun `isSuccessful returns false when both success and status are null`() {
        val response =
            ApiResponse(
                success = null,
                status = null,
                message = "Unknown",
                data = null,
            )
        assertFalse(response.isSuccessful())
    }

    @Test
    fun `isSuccessful returns true when both success is true and status is 200`() {
        val response =
            ApiResponse(
                success = true,
                status = 200,
                message = "Success",
                data = "test",
            )
        assertTrue(response.isSuccessful())
    }
}
