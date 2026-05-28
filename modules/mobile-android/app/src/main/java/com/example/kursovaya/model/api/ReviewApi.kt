package com.example.kursovaya.model.api

import com.google.gson.annotations.SerializedName

data class ReviewApi(
    val id: Long,
    @SerializedName("appointmentId")
    val appointmentId: Long,
    @SerializedName("doctorId")
    val doctorId: Long,
    @SerializedName("patientId")
    val patientId: Long,
    @SerializedName("patientName")
    val patientName: String?,
    val rating: Int,
    @SerializedName("reviewText")
    val reviewText: String?,
    @SerializedName("createdAt")
    val createdAt: String,
)
