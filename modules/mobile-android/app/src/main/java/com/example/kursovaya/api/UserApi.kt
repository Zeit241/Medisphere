package com.example.kursovaya.api

import com.example.kursovaya.model.api.ApiResponse
import com.example.kursovaya.model.api.User
import com.example.kursovaya.model.api.UserStats
import retrofit2.Response
import retrofit2.http.GET

interface UserApi {
    @GET("api/users/me")
    suspend fun getCurrentUser(): Response<ApiResponse<User>>

    @GET("api/users/userStats")
    suspend fun getUserStats(): Response<ApiResponse<UserStats>>
}
