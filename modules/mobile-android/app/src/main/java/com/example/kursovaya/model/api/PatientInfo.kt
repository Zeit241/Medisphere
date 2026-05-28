package com.example.kursovaya.model.api

data class PatientInfo(
    val id: Long,
    val birthDate: String?,
    val gender: Short?,
    val insuranceNumber: String?,
    val createdAt: String,
    val updatedAt: String,
)
