package com.example.kursovaya.api

import com.example.kursovaya.model.api.ApiResponse
import com.example.kursovaya.model.api.LoginRequest
import com.example.kursovaya.model.api.LoginResponse
import com.example.kursovaya.model.api.RegisterWithPatientRequest
import com.example.kursovaya.model.api.RegisterWithPatientResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest,
    ): Response<ApiResponse<LoginResponse>>

    @POST("api/auth/register-with-patient")
    suspend fun registerWithPatient(
        @Body request: RegisterWithPatientRequest,
    ): Response<ApiResponse<RegisterWithPatientResponse>>
}
