package com.example.kursovaya.api

import com.example.kursovaya.model.api.AppointmentApi
import com.example.kursovaya.model.api.BookAppointmentRequest
import com.example.kursovaya.model.api.CancelAppointmentRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AppointmentApi {
    @GET("api/appointments/patient/{patientId}")
    suspend fun getAppointmentsByPatientId(
        @Path("patientId") patientId: Long,
    ): Response<List<AppointmentApi>>

    @GET("api/appointments/{id}")
    suspend fun getAppointmentById(
        @Path("id") id: Long,
    ): Response<AppointmentApi>

    @GET("api/appointments/available")
    suspend fun getAvailableAppointments(
        @Query("doctorId") doctorId: Long,
        @Query("date") date: String,
        @Query("serviceId") serviceId: Long? = null,
    ): Response<List<AppointmentApi>>

    @POST("api/appointments/book")
    suspend fun bookAppointment(
        @Body request: BookAppointmentRequest,
    ): Response<AppointmentApi>

    @POST("api/appointments/{id}/cancel")
    suspend fun cancelAppointment(
        @Path("id") id: Long,
        @Body request: CancelAppointmentRequest?,
    ): Response<AppointmentApi>
}
