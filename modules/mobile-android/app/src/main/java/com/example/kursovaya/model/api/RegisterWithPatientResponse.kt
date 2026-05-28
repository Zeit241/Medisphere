package com.example.kursovaya.model.api

data class RegisterWithPatientResponse(
    val token: String,
    val patient: Patient,
    val message: String,
)
