package com.example.kursovaya.repository

import android.content.Context
import android.util.Log
import com.example.kursovaya.api.RetrofitClient
import com.example.kursovaya.model.api.DashboardResponse

class DashboardRepository(context: Context) {
    companion object {
        // Кэш для dashboard данных
        @Volatile
        private var cachedDashboard: DashboardResponse? = null

        // Время последнего обновления кэша
        @Volatile
        private var lastCacheTime: Long = 0

        // Время жизни кэша (5 минут)
        private const val CACHE_DURATION_MS = 5 * 60 * 1000L

        fun clearCache() {
            cachedDashboard = null
            lastCacheTime = 0
        }
    }

    init {
        RetrofitClient.init(context)
    }

    private val dashboardApi = RetrofitClient.dashboardApi

    /**
     * Получить dashboard данные.
     * @param forceRefresh если true, игнорирует кэш и загружает свежие данные
     * @return Result с DashboardResponse
     */
    suspend fun getDashboard(forceRefresh: Boolean = false): Result<DashboardResponse> {
        // Возвращаем кэш если он валиден и не требуется принудительное обновление
        if (!forceRefresh && isCacheValid()) {
            cachedDashboard?.let {
                Log.d("DashboardRepository", "Returning cached dashboard")
                return Result.success(it)
            }
        }

        return fetchDashboard()
    }

    /**
     * Получить кэшированные данные немедленно (если есть),
     * и обновить в фоне
     */
    fun getCachedDashboard(): DashboardResponse? {
        return cachedDashboard
    }

    /**
     * Проверить, есть ли валидный кэш
     */
    fun hasCachedData(): Boolean {
        return cachedDashboard != null
    }

    private fun isCacheValid(): Boolean {
        val currentTime = System.currentTimeMillis()
        return cachedDashboard != null && (currentTime - lastCacheTime) < CACHE_DURATION_MS
    }

    private suspend fun fetchDashboard(): Result<DashboardResponse> {
        return try {
            val response = dashboardApi.getDashboard()

            if (response.isSuccessful) {
                val dashboard = response.body()
                if (dashboard != null) {
                    // Обновляем кэш
                    cachedDashboard = dashboard
                    lastCacheTime = System.currentTimeMillis()
                    Log.d("DashboardRepository", "Dashboard fetched and cached")
                    Result.success(dashboard)
                } else {
                    Result.failure(Exception("Пустой ответ от сервера"))
                }
            } else {
                val errorMessage = "Ошибка получения dashboard: ${response.code()}"
                Log.e("DashboardRepository", errorMessage)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e("DashboardRepository", "Исключение при получении dashboard", e)
            Result.failure(e)
        }
    }
}
