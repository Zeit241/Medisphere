package com.example.kursovaya.model.api

import com.google.gson.annotations.SerializedName

data class AppointmentApi(
    val id: Long,
    @SerializedName("scheduleId")
    val scheduleId: Long?,
    @SerializedName("doctorId")
    val doctorId: Long,
    @SerializedName("patientId")
    val patientId: Long?,
    @SerializedName("roomId")
    val roomId: Long?,
    @SerializedName("startTime")
    val startTime: String,
    @SerializedName("endTime")
    val endTime: String,
    @SerializedName("isBooked")
    val isBooked: Boolean?,
    val status: String,
    val source: String?,
    @SerializedName("createdBy")
    val createdBy: Long?,
    @SerializedName("createdAt")
    val createdAt: String,
    @SerializedName("updatedAt")
    val updatedAt: String,
    @SerializedName("cancelReason")
    val cancelReason: String?,
    @SerializedName("diagnosis")
    val diagnosis: String?,
    val service: AppointmentServiceApi? = null,
)

/** Вложенная услуга в ответе записи (совпадает с полем service в JSON) */
data class AppointmentServiceApi(
    val id: Long? = null,
    val name: String? = null,
    val code: String? = null,
    val price: Double? = null,
    @SerializedName("durationMinutes") val durationMinutes: Int? = null,
    val description: String? = null,
)
