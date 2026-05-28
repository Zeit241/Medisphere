package com.example.kursovaya.model.api

import com.google.gson.annotations.SerializedName

data class AiCatalogResponse(
    val doctors: List<AiDoctorRef> = emptyList(),
    val services: List<AiServiceRef> = emptyList(),
)

data class AiDoctorRef(
    val id: Long,
    val name: String,
    val specialization: String,
)

data class AiServiceRef(
    val id: Long,
    val name: String,
)

/** Элемент GET /api/services для врача */
data class ClinicServiceItem(
    val id: Long? = null,
    val name: String? = null,
    val price: Double? = null,
    @SerializedName("durationMinutes") val durationMinutes: Int? = null,
)
