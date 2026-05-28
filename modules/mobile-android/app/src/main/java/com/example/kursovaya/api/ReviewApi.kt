package com.example.kursovaya.api

import com.example.kursovaya.model.api.ApiResponse
import com.example.kursovaya.model.api.CreateReviewRequest
import com.example.kursovaya.model.api.ReviewApi
import com.example.kursovaya.model.api.UpdateReviewRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ReviewApi {
    @POST("api/reviews")
    suspend fun createReview(
        @Body request: CreateReviewRequest,
    ): Response<ApiResponse<ReviewApi>>

    @GET("api/reviews/appointment/{appointmentId}")
    suspend fun getReviewByAppointmentId(
        @Path("appointmentId") appointmentId: Long,
    ): Response<ReviewApi>

    @PUT("api/reviews/{id}")
    suspend fun updateReview(
        @Path("id") id: Long,
        @Body request: UpdateReviewRequest,
    ): Response<ApiResponse<ReviewApi>>
}
