package com.example.kursovaya.repository

import android.util.Log
import com.example.kursovaya.api.RetrofitClient
import com.example.kursovaya.model.api.EmergencyContact
import com.example.kursovaya.model.api.LoginRequest
import com.example.kursovaya.model.api.LoginResponse
import com.example.kursovaya.model.api.RegisterWithPatientRequest
import com.example.kursovaya.model.api.RegisterWithPatientResponse

class AuthApiRepository {
    private val authApi = RetrofitClient.authApi

    suspend fun login(
        email: String,
        password: String,
    ): Result<LoginResponse> {
        return try {
            val request = LoginRequest(email, password)
            val response = authApi.login(request)

            Log.d("AuthApiRepository", "Response isSuccessful: ${response.isSuccessful}")
            Log.d("AuthApiRepository", "Response body: ${response.body()}")
            Log.d("AuthApiRepository", "Response body isSuccessful(): ${response.body()?.isSuccessful()}")

            if (response.isSuccessful && response.body()?.isSuccessful() == true) {
                val loginResponse = response.body()?.data
                Log.d("AuthApiRepository", "LoginResponse: $loginResponse")
                if (loginResponse != null) {
                    Result.success(loginResponse)
                } else {
                    Log.e("AuthApiRepository", "Пустой ответ от сервера")
                    Result.failure(Exception("Пустой ответ от сервера"))
                }
            } else {
                val errorMessage = response.body()?.message ?: "Ошибка авторизации"
                Log.e("AuthApiRepository", "Ошибка: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e("AuthApiRepository", "Исключение при входе", e)
            Result.failure(e)
        }
    }

    suspend fun registerWithPatient(
        email: String,
        phone: String,
        password: String,
        confirmPassword: String,
        fio: String,
        birthDate: String, // Формат: YYYY-MM-DD
        gender: Short, // 1 = мужской, 2 = женский
        insuranceNumber: String? = null,
        emergencyContact: EmergencyContact? = null,
    ): Result<RegisterWithPatientResponse> {
        return try {
            val request =
                RegisterWithPatientRequest(
                    email = email,
                    phone = phone,
                    password = password,
                    confirmPassword = confirmPassword,
                    fio = fio,
                    birthDate = birthDate,
                    gender = gender,
                    insuranceNumber = insuranceNumber,
                    emergencyContact = emergencyContact,
                )
            val response = authApi.registerWithPatient(request)

            if (response.isSuccessful && response.body()?.isSuccessful() == true) {
                val registerResponse = response.body()?.data
                if (registerResponse != null) {
                    Result.success(registerResponse)
                } else {
                    Result.failure(Exception("Пустой ответ от сервера"))
                }
            } else {
                val errorMessage = response.body()?.message ?: "Ошибка регистрации"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
