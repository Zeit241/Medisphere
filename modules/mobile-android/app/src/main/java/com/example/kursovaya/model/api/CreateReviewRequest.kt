package com.example.kursovaya.model.api

import com.google.gson.annotations.SerializedName

data class CreateReviewRequest(
    val appointment: AppointmentRef,
    val doctor: DoctorRef,
    val patient: PatientRef,
    val rating: Int,
    @SerializedName("reviewText")
    val reviewText: String?,
)

data class AppointmentRef(
    val id: Long,
)

data class DoctorRef(
    val id: Long,
)

data class PatientRef(
    val id: Long,
)
