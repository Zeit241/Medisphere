package com.example.kursovaya.model.api

data class RegisterRequest(
    val email: String,
    val phone: String,
    val password: String,
    val confirmPassword: String,
    val fio: String,
)
