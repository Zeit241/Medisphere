package com.example.kursovaya.model.api

data class User(
    val id: Long,
    val email: String,
    val phone: String,
    val firstName: String,
    val lastName: String,
    val middleName: String?,
    val createdAt: String,
    val updatedAt: String,
    val active: Boolean,
    val patientId: Long? = null,
    val doctorId: Long? = null,
    val patient: PatientInfo? = null,
    val doctor: DoctorInfo? = null,
)

data class DoctorInfo(
    val id: Long,
    val displayName: String?,
    val bio: String?,
    val experienceYears: Int?,
    val specializations: List<Specialization>?,
    val createdAt: String,
    val updatedAt: String,
)
