package com.example.kursovaya.model

sealed class AuthState {
    object Unauthenticated : AuthState()

    data class Authenticated(val token: String) : AuthState()
}
