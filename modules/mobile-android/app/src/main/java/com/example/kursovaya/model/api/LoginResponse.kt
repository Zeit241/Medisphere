package com.example.kursovaya.model.api

data class LoginResponse(
    val token: String,
    val email: String,
    val message: String,
)
