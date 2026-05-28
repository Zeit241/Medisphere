package com.example.kursovaya.api

import com.example.kursovaya.model.api.DashboardResponse
import retrofit2.Response
import retrofit2.http.GET

interface DashboardApi {
    @GET("api/dashboard")
    suspend fun getDashboard(): Response<DashboardResponse>
}
