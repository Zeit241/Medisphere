package com.example.kursovaya.api

import com.example.kursovaya.model.api.ApiResponse
import com.example.kursovaya.model.api.DoctorApi
import com.example.kursovaya.model.api.ReviewApi
import com.example.kursovaya.model.api.Specialization
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface DoctorsApi {
    @GET("api/doctors")
    suspend fun getDoctors(
        @Query("q") query: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null,
        @Query("sortBy") sortBy: String? = null,
        @Query("sortOrder") sortOrder: String? = null,
    ): Response<ApiResponse<List<DoctorApi>>>

    @GET("api/doctors/{id}")
    suspend fun getDoctorById(
        @retrofit2.http.Path("id") id: Long,
    ): Response<ApiResponse<DoctorApi>>

    @GET("api/specializations")
    suspend fun getSpecializations(): Response<List<Specialization>>

    @GET("api/reviews/doctor/{doctorId}")
    suspend fun getDoctorReviews(
        @Path("doctorId") doctorId: Long,
    ): Response<List<ReviewApi>>
}
