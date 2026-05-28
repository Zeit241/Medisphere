package com.example.kursovaya.model.api

import com.google.gson.annotations.SerializedName

data class UpdateReviewRequest(
    val rating: Int?,
    @SerializedName("reviewText")
    val reviewText: String?,
)
