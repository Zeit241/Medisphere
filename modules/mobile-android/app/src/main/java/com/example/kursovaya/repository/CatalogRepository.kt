package com.example.kursovaya.repository

import android.content.Context
import android.util.Log
import com.example.kursovaya.api.RetrofitClient
import com.example.kursovaya.model.api.AiCatalogResponse

class CatalogRepository(context: Context) {
    init {
        RetrofitClient.init(context)
    }

    private val catalogApi = RetrofitClient.catalogApi
    private val clinicServicesApi = RetrofitClient.clinicServicesApi

    suspend fun getAiReferenceCatalog(): Result<AiCatalogResponse> {
        return try {
            val response = catalogApi.getAiReference()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Пустой каталог"))
                }
            } else {
                Result.failure(Exception("Каталог: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("CatalogRepository", "Ошибка каталога", e)
            Result.failure(e)
        }
    }

    suspend fun getServicesForDoctor(doctorId: Long): Result<List<com.example.kursovaya.model.api.ClinicServiceItem>> {
        return try {
            val response = clinicServicesApi.getServicesForDoctor(doctorId)
            if (response.isSuccessful) {
                Result.success(response.body().orEmpty())
            } else {
                Result.failure(Exception("Услуги: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("CatalogRepository", "Ошибка услуг", e)
            Result.failure(e)
        }
    }
}
