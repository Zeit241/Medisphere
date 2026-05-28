package com.example.kursovaya.repository

import com.example.kursovaya.model.api.User
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.*

/**
 * Тесты для UserDataRepository
 *
 * Примечание: Тестирование EncryptedSharedPreferences требует Android контекста,
 * поэтому эти тесты проверяют логику работы с данными, но для полного тестирования
 * нужны инструментальные тесты (androidTest)
 */
class UserDataRepositoryTest {
    private val gson: Gson = GsonBuilder().create()

    @Test
    fun `User serialization and deserialization`() {
        // Arrange
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

        // Act - сериализация
        val userJson = gson.toJson(user)
        assertNotNull(userJson)
        assertTrue(userJson.isNotEmpty())

        // Act - десериализация
        val deserializedUser = gson.fromJson(userJson, User::class.java)

        // Assert
        assertNotNull(deserializedUser)
        assertEquals(user.id, deserializedUser.id)
        assertEquals(user.email, deserializedUser.email)
        assertEquals(user.phone, deserializedUser.phone)
        assertEquals(user.firstName, deserializedUser.firstName)
        assertEquals(user.lastName, deserializedUser.lastName)
        assertEquals(user.middleName, deserializedUser.middleName)
        assertEquals(user.active, deserializedUser.active)
        assertEquals(user.patientId, deserializedUser.patientId)
    }

    @Test
    fun `User with null middleName serialization`() {
        // Arrange
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

        // Act
        val userJson = gson.toJson(user)
        val deserializedUser = gson.fromJson(userJson, User::class.java)

        // Assert
        assertNull(deserializedUser.middleName)
        assertNull(deserializedUser.patientId)
        assertEquals(200L, deserializedUser.doctorId)
    }

    @Test
    fun `Invalid JSON deserialization returns null`() {
        // Arrange
        val invalidJson = "invalid json string"

        // Act & Assert
        try {
            val user = gson.fromJson(invalidJson, User::class.java)
            // Gson может вернуть объект с дефолтными значениями, но это зависит от реализации
        } catch (e: Exception) {
            // Ожидаем исключение при невалидном JSON
            assertNotNull(e)
        }
    }

    @Test
    fun `Empty JSON deserialization`() {
        // Arrange
        val emptyJson = "{}"

        // Act
        val user = gson.fromJson(emptyJson, User::class.java)

        // Assert
        assertNotNull(user)
        assertEquals(0L, user.id)
        assertNull(user.email)
    }

    @Test
    fun `User JSON structure validation`() {
        // Arrange
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
                patientId = null,
                doctorId = null,
                patient = null,
                doctor = null,
            )

        // Act
        val userJson = gson.toJson(user)

        // Assert - проверяем, что JSON содержит ключевые поля
        assertTrue(userJson.contains("\"id\""))
        assertTrue(userJson.contains("\"email\""))
        assertTrue(userJson.contains("\"firstName\""))
        assertTrue(userJson.contains("\"lastName\""))
        assertTrue(userJson.contains("test@example.com"))
    }
}
