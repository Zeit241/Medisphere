package com.example.kursovaya.model.api

data class RegisterWithPatientRequest(
    val email: String,
    val phone: String,
    val password: String,
    val confirmPassword: String,
    val fio: String,
    val birthDate: String, // Формат: YYYY-MM-DD
    val gender: Short, // 1 = мужской, 2 = женский
    val insuranceNumber: String? = null, // Опционально
    val emergencyContact: EmergencyContact? = null, // Опционально
)
