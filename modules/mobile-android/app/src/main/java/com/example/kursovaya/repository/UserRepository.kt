package com.example.kursovaya.repository

import android.content.Context
import android.util.Log
import com.example.kursovaya.api.RetrofitClient
import com.example.kursovaya.api.UserApi
import com.example.kursovaya.model.api.User
import com.example.kursovaya.model.api.UserStats

class UserRepository(context: Context) {
    init {
        RetrofitClient.init(context)
        UserDataRepository.init(context)
    }

    private val userApi: UserApi
        get() = RetrofitClient.userApi

    suspend fun getCurrentUser(): Result<User> {
        return try {
            Log.d("UserRepository", "Запрос данных текущего пользователя...")
            val response = userApi.getCurrentUser()

            Log.d("UserRepository", "Response code: ${response.code()}")
            Log.d("UserRepository", "Response isSuccessful: ${response.isSuccessful}")
            Log.d("UserRepository", "Response body: ${response.body()}")

            if (response.isSuccessful && response.body()?.isSuccessful() == true) {
                val user = response.body()?.data
                if (user != null) {
                    Log.d("UserRepository", "Данные пользователя получены успешно")
                    // Сохраняем пользователя в хранилище
                    UserDataRepository.saveUser(user)
                    Result.success(user)
                } else {
                    Log.e("UserRepository", "Пустой ответ от сервера")
                    Result.failure(Exception("Пустой ответ от сервера"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("UserRepository", "Error body: $errorBody")
                val errorMessage = response.body()?.message ?: errorBody ?: "Ошибка получения данных пользователя: ${response.code()}"
                Log.e("UserRepository", errorMessage)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Исключение при получении данных пользователя", e)
            Result.failure(e)
        }
    }

    suspend fun getUserStats(): Result<UserStats> {
        return try {
            Log.d("UserRepository", "Запрос статистики пользователя...")
            val response = userApi.getUserStats()

            Log.d("UserRepository", "UserStats Response code: ${response.code()}")
            Log.d("UserRepository", "UserStats Response isSuccessful: ${response.isSuccessful}")

            if (response.isSuccessful && response.body()?.isSuccessful() == true) {
                val stats = response.body()?.data
                if (stats != null) {
                    Log.d(
                        "UserRepository",
                        "Статистика пользователя получена: appointments=${stats.appointmentsCount}, reviews=${stats.reviewsCount}, queues=${stats.queueEntriesCount}",
                    )
                    Result.success(stats)
                } else {
                    Log.e("UserRepository", "Пустой ответ от сервера (статистика)")
                    Result.failure(Exception("Пустой ответ от сервера"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("UserRepository", "Error body (stats): $errorBody")
                val errorMessage = response.body()?.message ?: errorBody ?: "Ошибка получения статистики: ${response.code()}"
                Log.e("UserRepository", errorMessage)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Исключение при получении статистики пользователя", e)
            Result.failure(e)
        }
    }
}
