package com.example.kursovaya.api

import com.example.kursovaya.model.api.ClinicServiceItem
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ClinicServicesApi {
    @GET("api/services")
    suspend fun getServicesForDoctor(
        @Query("doctorId") doctorId: Long,
    ): Response<List<ClinicServiceItem>>
}
