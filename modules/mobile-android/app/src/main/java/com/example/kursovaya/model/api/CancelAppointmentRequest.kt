package com.example.kursovaya.model.api

import com.google.gson.annotations.SerializedName

data class CancelAppointmentRequest(
    @SerializedName("cancelReason")
    val cancelReason: String?,
)
