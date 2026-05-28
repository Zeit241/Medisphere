package com.example.kursovaya.model.api

data class BookAppointmentRequest(
    val appointmentId: Long,
    val userId: Long,
    val serviceId: Long? = null,
)
