package com.example.kursovaya.model.api

data class Patient(
    val id: Long,
    val user: User,
    val birthDate: String,
    val gender: Short,
    val insuranceNumber: String?,
    val emergencyContact: String?,
    val createdAt: String,
    val updatedAt: String,
)
