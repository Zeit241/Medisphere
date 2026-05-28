package com.example.kursovaya.repository

import android.content.Context
import android.util.Log
import com.example.kursovaya.api.RetrofitClient
import com.example.kursovaya.model.api.AppointmentRef
import com.example.kursovaya.model.api.CreateReviewRequest
import com.example.kursovaya.model.api.DoctorRef
import com.example.kursovaya.model.api.PatientRef
import com.example.kursovaya.model.api.ReviewApi
import com.example.kursovaya.model.api.UpdateReviewRequest

class ReviewRepository(context: Context) {
    init {
        RetrofitClient.init(context)
    }

    private val reviewApi
        get() = RetrofitClient.reviewApi

    suspend fun createReview(
        appointmentId: Long,
        doctorId: Long,
        patientId: Long,
        rating: Int,
        reviewText: String?,
    ): Result<ReviewApi> {
        return try {
            Log.d("ReviewRepository", "Создание отзыва для записи $appointmentId...")

            val request =
                CreateReviewRequest(
                    appointment = AppointmentRef(appointmentId),
                    doctor = DoctorRef(doctorId),
                    patient = PatientRef(patientId),
                    rating = rating,
                    reviewText = if (reviewText.isNullOrEmpty()) null else reviewText,
                )

            val response = reviewApi.createReview(request)

            Log.d("ReviewRepository", "Response code: ${response.code()}")
            Log.d("ReviewRepository", "Response isSuccessful: ${response.isSuccessful}")

            // Код 200 означает успех
            if (response.code() == 200 || (response.isSuccessful && response.body()?.isSuccessful() == true)) {
                val review = response.body()?.data
                if (review != null) {
                    Log.d("ReviewRepository", "Отзыв создан успешно")
                    Result.success(review)
                } else {
                    // Даже если data пустой, но код 200 - считаем успехом
                    Log.d("ReviewRepository", "Ответ успешен (код 200), но data пустой. Создаем пустой ReviewApi.")
                    // Возвращаем успех, даже если data пустой
                    Result.success(ReviewApi(0, appointmentId, doctorId, patientId, null, rating, reviewText, ""))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("ReviewRepository", "Error body: $errorBody")
                val errorMessage = response.body()?.message ?: errorBody ?: "Ошибка создания отзыва: ${response.code()}"
                Log.e("ReviewRepository", errorMessage)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e("ReviewRepository", "Исключение при создании отзыва", e)
            Result.failure(e)
        }
    }

    suspend fun getReviewByAppointmentId(appointmentId: Long): Result<ReviewApi?> {
        return try {
            Log.d("ReviewRepository", "Получение отзыва для записи $appointmentId...")

            val response = reviewApi.getReviewByAppointmentId(appointmentId)

            Log.d("ReviewRepository", "Response code: ${response.code()}")
            Log.d("ReviewRepository", "Response isSuccessful: ${response.isSuccessful}")

            when {
                response.isSuccessful && response.body() != null -> {
                    val review = response.body()
                    Log.d("ReviewRepository", "Отзыв найден: rating=${review?.rating}")
                    Result.success(review)
                }
                response.code() == 404 -> {
                    Log.d("ReviewRepository", "Отзыв не найден для записи $appointmentId")
                    Result.success(null) // Отзыв не найден - это нормальная ситуация
                }
                else -> {
                    val errorBody = response.errorBody()?.string()
                    Log.e("ReviewRepository", "Error body: $errorBody")
                    val errorMessage = errorBody ?: "Ошибка получения отзыва: ${response.code()}"
                    Log.e("ReviewRepository", errorMessage)
                    Result.failure(Exception(errorMessage))
                }
            }
        } catch (e: Exception) {
            Log.e("ReviewRepository", "Исключение при получении отзыва", e)
            Result.failure(e)
        }
    }

    suspend fun updateReview(
        reviewId: Long,
        rating: Int?,
        reviewText: String?,
    ): Result<ReviewApi> {
        return try {
            Log.d("ReviewRepository", "Обновление отзыва $reviewId...")

            val request =
                UpdateReviewRequest(
                    rating = rating,
                    reviewText = if (reviewText.isNullOrEmpty()) null else reviewText,
                )

            val response = reviewApi.updateReview(reviewId, request)

            Log.d("ReviewRepository", "Response code: ${response.code()}")
            Log.d("ReviewRepository", "Response isSuccessful: ${response.isSuccessful}")

            // Код 200 означает успех
            if (response.code() == 200 || (response.isSuccessful && response.body()?.isSuccessful() == true)) {
                val review = response.body()?.data
                if (review != null) {
                    Log.d("ReviewRepository", "Отзыв обновлен успешно")
                    Result.success(review)
                } else {
                    // Даже если data пустой, но код 200 - считаем успехом
                    Log.d("ReviewRepository", "Ответ успешен (код 200), но data пустой. Создаем ReviewApi с переданными данными.")
                    // Возвращаем успех с данными из запроса
                    Result.success(ReviewApi(reviewId, 0, 0, 0, null, rating ?: 0, reviewText, ""))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("ReviewRepository", "Error body: $errorBody")
                val errorMessage = response.body()?.message ?: errorBody ?: "Ошибка обновления отзыва: ${response.code()}"
                Log.e("ReviewRepository", errorMessage)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e("ReviewRepository", "Исключение при обновлении отзыва", e)
            Result.failure(e)
        }
    }
}
