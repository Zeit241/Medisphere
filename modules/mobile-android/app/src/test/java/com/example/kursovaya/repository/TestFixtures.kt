package com.example.kursovaya.repository

import com.example.kursovaya.model.api.AppointmentApi
import com.example.kursovaya.model.api.DoctorApi
import com.example.kursovaya.model.api.Specialization
import com.example.kursovaya.model.api.User

object TestFixtures {
    fun user(id: Long = 1L) =
        User(
            id = id,
            email = "user@example.com",
            phone = "+79990001122",
            firstName = "Ivan",
            lastName = "Petrov",
            middleName = null,
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:00:00Z",
            active = true,
        )

    fun doctor(id: Long = 3L) =
        DoctorApi(
            id = id,
            user = user(id),
            displayName = "Ivan Petrov",
            bio = "Bio",
            experienceYears = 5,
            photoUrl = null,
            rating = 4.5,
            reviewCount = 2,
            specializations = listOf(Specialization(1, "THER", "Therapy", null)),
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:00:00Z",
        )

    fun appointmentApi(id: Long = 1L) =
        AppointmentApi(
            id = id,
            scheduleId = 2L,
            doctorId = 3L,
            patientId = 4L,
            roomId = 5L,
            startTime = "2026-03-01T10:00:00Z",
            endTime = "2026-03-01T10:30:00Z",
            isBooked = true,
            status = "scheduled",
            source = "web",
            createdBy = 4L,
            createdAt = "2026-02-01T00:00:00Z",
            updatedAt = "2026-02-01T00:00:00Z",
            cancelReason = null,
            diagnosis = null,
            service = null,
        )
}
