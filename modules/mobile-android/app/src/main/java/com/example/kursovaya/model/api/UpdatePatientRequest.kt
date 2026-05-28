package com.example.kursovaya.model.api

data class UpdatePatientRequest(
    val user: UpdateUserRequest,
    val birthDate: String, // Формат: YYYY-MM-DD
    val gender: Short, // 1 = мужской, 2 = женский
    val insuranceNumber: String?,
)

data class UpdateUserRequest(
    val email: String,
    val phone: String,
    val firstName: String,
    val lastName: String,
    val middleName: String?,
)
