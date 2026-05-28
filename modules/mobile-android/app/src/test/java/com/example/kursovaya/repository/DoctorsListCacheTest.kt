package com.example.kursovaya.repository

import com.example.kursovaya.model.api.DoctorApi
import com.example.kursovaya.model.api.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class DoctorsListCacheTest {
    @Before
    fun clearCache() {
        DoctorsListCache.invalidateAll()
    }

    private fun doctor(id: Long) =
        DoctorApi(
            id = id,
            user =
                User(
                    id = id,
                    email = "a@example.com",
                    phone = "+7999",
                    firstName = "Ivan",
                    lastName = "Petrov",
                    middleName = null,
                    createdAt = "2026-01-01T00:00:00Z",
                    updatedAt = "2026-01-01T00:00:00Z",
                    active = true,
                ),
            displayName = "Ivan Petrov",
            bio = null,
            experienceYears = 5,
            photoUrl = null,
            rating = 4.5,
            reviewCount = 2,
            specializations = emptyList(),
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:00:00Z",
        )

    @Test
    fun putAndGet_returnsCachedDoctors() {
        val list = listOf(doctor(1L))
        DoctorsListCache.put(null, 10, 0, list)
        assertEquals(list, DoctorsListCache.get(null, 10, 0))
    }

    @Test
    fun get_returnsNullForUnknownKey() {
        assertNull(DoctorsListCache.get("query", 5, 0))
    }

    @Test
    fun invalidateAll_clearsEntries() {
        DoctorsListCache.put(null, 10, 0, listOf(doctor(2L)))
        DoctorsListCache.invalidateAll()
        assertNull(DoctorsListCache.get(null, 10, 0))
    }

    @Test
    fun cacheKey_differsByOffset() {
        DoctorsListCache.put(null, 10, 0, listOf(doctor(1L)))
        DoctorsListCache.put(null, 10, 10, listOf(doctor(2L)))
        assertEquals(1L, DoctorsListCache.get(null, 10, 0)!!.first().id)
        assertEquals(2L, DoctorsListCache.get(null, 10, 10)!!.first().id)
    }
}
