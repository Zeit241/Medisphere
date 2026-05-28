package com.example.kursovaya.api

import com.example.kursovaya.model.api.AiCatalogResponse
import retrofit2.Response
import retrofit2.http.GET

interface CatalogApi {
    @GET("api/catalog/ai-reference")
    suspend fun getAiReference(): Response<AiCatalogResponse>
}
