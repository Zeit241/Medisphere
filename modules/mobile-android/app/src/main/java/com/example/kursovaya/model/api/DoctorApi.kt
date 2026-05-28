package com.example.kursovaya.model.api

import com.google.gson.annotations.SerializedName

data class DoctorApi(
    val id: Long,
    val user: User,
    val displayName: String?,
    val bio: String?,
    val experienceYears: Int,
    @SerializedName("photo")
    val photoUrl: String?,
    val rating: Double?,
    val reviewCount: Int?,
    val specializations: List<Specialization>?,
    val createdAt: String,
    val updatedAt: String,
)
