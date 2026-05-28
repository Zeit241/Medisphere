package com.example.kursovaya.model.api

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName("success")
    val success: Boolean? = null,
    @SerializedName("status")
    val status: Int? = null,
    val message: String,
    val data: T?,
) {
    // Проверяем успешность через success или status
    fun isSuccessful(): Boolean {
        return success == true || status == 200
    }
}
