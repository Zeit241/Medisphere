package com.example.kursovaya.repository

import android.content.Context
import android.util.Log
import com.example.kursovaya.api.RetrofitClient
import com.example.kursovaya.model.Appointment
import com.example.kursovaya.model.api.AppointmentApi
import com.example.kursovaya.model.api.BookAppointmentRequest
import com.example.kursovaya.model.api.CancelAppointmentRequest
import com.example.kursovaya.model.api.DoctorApi
import com.example.kursovaya.model.api.toAppointment

class AppointmentRepository(context: Context) {
    init {
        RetrofitClient.init(context)
    }

    private val appointmentApi = RetrofitClient.appointmentApi
    private val doctorsRepository = DoctorsRepository(context)

    suspend fun getAppointmentsByPatientId(patientId: Long): Result<List<AppointmentApi>> {
        return try {
            Log.d("AppointmentRepository", "Запрос записей для пациента $patientId...")
            val response = appointmentApi.getAppointmentsByPatientId(patientId)

            Log.d("AppointmentRepository", "Response code: ${response.code()}")
            Log.d("AppointmentRepository", "Response isSuccessful: ${response.isSuccessful}")

            if (response.isSuccessful) {
                val appointments = response.body()
                if (appointments != null) {
                    Log.d("AppointmentRepository", "Получено ${appointments.size} записей")
                    Result.success(appointments)
                } else {
                    Log.e("AppointmentRepository", "Пустой ответ от сервера")
                    Result.failure(Exception("Пустой ответ от сервера"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("AppointmentRepository", "Error body: $errorBody")
                val errorMessage = errorBody ?: "Ошибка получения записей: ${response.code()}"
                Log.e("AppointmentRepository", errorMessage)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e("AppointmentRepository", "Исключение при получении записей", e)
            Result.failure(e)
        }
    }

    suspend fun getAppointmentById(id: Long): Result<AppointmentApi> {
        return try {
            Log.d("AppointmentRepository", "Запрос записи $id...")
            val response = appointmentApi.getAppointmentById(id)

            Log.d("AppointmentRepository", "Response code: ${response.code()}")
            Log.d("AppointmentRepository", "Response isSuccessful: ${response.isSuccessful}")

            if (response.isSuccessful) {
                val appointment = response.body()
                if (appointment != null) {
                    Log.d("AppointmentRepository", "Запись получена успешно")
                    Result.success(appointment)
                } else {
                    Log.e("AppointmentRepository", "Пустой ответ от сервера")
                    Result.failure(Exception("Пустой ответ от сервера"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("AppointmentRepository", "Error body: $errorBody")
                val errorMessage = errorBody ?: "Ошибка получения записи: ${response.code()}"
                Log.e("AppointmentRepository", errorMessage)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e("AppointmentRepository", "Исключение при получении записи", e)
            Result.failure(e)
        }
    }

    suspend fun getDoctorById(doctorId: Long): Result<DoctorApi> {
        return doctorsRepository.getDoctorById(doctorId)
    }

    /**
     * Получает все записи пациента и конвертирует их в Appointment
     */
    suspend fun getAppointmentsForPatient(patientId: Long): Result<List<Appointment>> {
        return try {
            val appointmentsApiResult = getAppointmentsByPatientId(patientId)
            if (appointmentsApiResult.isFailure) {
                return Result.failure(appointmentsApiResult.exceptionOrNull() ?: Exception("Ошибка получения записей"))
            }

            val appointmentsApi = appointmentsApiResult.getOrNull() ?: return Result.success(emptyList())
            val appointments = mutableListOf<Appointment>()

            for (appointmentApi in appointmentsApi) {
                val doctorResult = getDoctorById(appointmentApi.doctorId.toLong())
                if (doctorResult.isSuccess) {
                    val doctor = doctorResult.getOrNull()!!
                    // Формируем полное ФИО: Фамилия Имя Отчество
                    val doctorName =
                        buildString {
                            append(doctor.user.lastName)
                            if (doctor.user.firstName.isNotEmpty()) {
                                append(" ${doctor.user.firstName}")
                            }
                            if (!doctor.user.middleName.isNullOrEmpty()) {
                                append(" ${doctor.user.middleName}")
                            }
                        }.trim().ifEmpty { doctor.user.email }
                    val specialty = doctor.specializations?.firstOrNull()?.name ?: "Врач"

                    val appointment =
                        appointmentApi.toAppointment(
                            doctorName = doctorName,
                            doctorSpecialty = specialty,
                            doctorPhone = doctor.user.phone,
                            doctorEmail = doctor.user.email,
                            doctorImage = doctor.photoUrl,
                            doctorRating = doctor.rating?.toFloat() ?: 0f,
                            doctorReviewCount = doctor.reviewCount ?: 0,
                            doctorExperienceYears = doctor.experienceYears ?: 0,
                            doctorBio = doctor.bio,
                            roomCode = appointmentApi.roomId?.toString() ?: "N/A",
                            roomName = "Кабинет",
                        )
                    appointments.add(appointment)
                } else {
                    Log.e("AppointmentRepository", "Не удалось получить данные врача ${appointmentApi.doctorId}")
                }
            }

            Result.success(appointments)
        } catch (e: Exception) {
            Log.e("AppointmentRepository", "Исключение при конвертации записей", e)
            Result.failure(e)
        }
    }

    /**
     * Получает запись по ID и конвертирует её в Appointment
     */
    suspend fun getAppointmentForPatient(appointmentId: Long): Result<Appointment> {
        return try {
            val appointmentApiResult = getAppointmentById(appointmentId)
            if (appointmentApiResult.isFailure) {
                return Result.failure(appointmentApiResult.exceptionOrNull() ?: Exception("Ошибка получения записи"))
            }

            val appointmentApi = appointmentApiResult.getOrNull() ?: return Result.failure(Exception("Запись не найдена"))

            val doctorResult = getDoctorById(appointmentApi.doctorId.toLong())
            if (doctorResult.isFailure) {
                return Result.failure(doctorResult.exceptionOrNull() ?: Exception("Ошибка получения данных врача"))
            }

            val doctor = doctorResult.getOrNull()!!
            // Формируем полное ФИО: Фамилия Имя Отчество
            val doctorName =
                buildString {
                    append(doctor.user.lastName)
                    if (doctor.user.firstName.isNotEmpty()) {
                        append(" ${doctor.user.firstName}")
                    }
                    if (!doctor.user.middleName.isNullOrEmpty()) {
                        append(" ${doctor.user.middleName}")
                    }
                }.trim().ifEmpty { doctor.user.email }
            val specialty = doctor.specializations?.firstOrNull()?.name ?: "Врач"

            val appointment =
                appointmentApi.toAppointment(
                    doctorName = doctorName,
                    doctorSpecialty = specialty,
                    doctorPhone = doctor.user.phone,
                    doctorEmail = doctor.user.email,
                    doctorImage = doctor.photoUrl,
                    doctorRating = doctor.rating?.toFloat() ?: 0f,
                    doctorReviewCount = doctor.reviewCount ?: 0,
                    doctorExperienceYears = doctor.experienceYears ?: 0,
                    doctorBio = doctor.bio,
                    roomCode = appointmentApi.roomId?.toString() ?: "N/A",
                    roomName = "Кабинет",
                )

            Result.success(appointment)
        } catch (e: Exception) {
            Log.e("AppointmentRepository", "Исключение при конвертации записи", e)
            Result.failure(e)
        }
    }

    /**
     * Получает список доступного времени записи для врача на указанную дату
     */
    suspend fun getAvailableAppointments(
        doctorId: Long,
        date: String,
        serviceId: Long? = null,
    ): Result<List<AppointmentApi>> {
        return try {
            Log.d(
                "AppointmentRepository",
                "Запрос доступного времени для врача $doctorId на дату $date, serviceId=$serviceId...",
            )
            val response = appointmentApi.getAvailableAppointments(doctorId, date, serviceId)

            Log.d("AppointmentRepository", "Response code: ${response.code()}")
            Log.d("AppointmentRepository", "Response isSuccessful: ${response.isSuccessful}")

            if (response.isSuccessful) {
                val appointments = response.body()
                if (appointments != null) {
                    Log.d("AppointmentRepository", "Получено ${appointments.size} доступных слотов")
                    Result.success(appointments)
                } else {
                    Log.e("AppointmentRepository", "Пустой ответ от сервера")
                    Result.failure(Exception("Пустой ответ от сервера"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("AppointmentRepository", "Error body: $errorBody")
                val errorMessage = errorBody ?: "Ошибка получения доступного времени: ${response.code()}"
                Log.e("AppointmentRepository", errorMessage)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e("AppointmentRepository", "Исключение при получении доступного времени", e)
            Result.failure(e)
        }
    }

    /**
     * Бронирует запись на прием
     */
    suspend fun bookAppointment(
        appointmentId: Long,
        userId: Long,
        serviceId: Long? = null,
    ): Result<AppointmentApi> {
        return try {
            Log.d(
                "AppointmentRepository",
                "Бронирование записи $appointmentId для пользователя $userId, serviceId=$serviceId...",
            )
            val request = BookAppointmentRequest(appointmentId, userId, serviceId)
            val response = appointmentApi.bookAppointment(request)

            Log.d("AppointmentRepository", "Response code: ${response.code()}")
            Log.d("AppointmentRepository", "Response isSuccessful: ${response.isSuccessful}")

            if (response.isSuccessful) {
                val appointment = response.body()
                if (appointment != null) {
                    Log.d("AppointmentRepository", "Запись успешно забронирована")
                    Result.success(appointment)
                } else {
                    Log.e("AppointmentRepository", "Пустой ответ от сервера")
                    Result.failure(Exception("Пустой ответ от сервера"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("AppointmentRepository", "Error body: $errorBody")
                val errorMessage = errorBody ?: "Ошибка бронирования записи: ${response.code()}"
                Log.e("AppointmentRepository", errorMessage)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e("AppointmentRepository", "Исключение при бронировании записи", e)
            Result.failure(e)
        }
    }

    /**
     * Отменяет запись на приём
     */
    suspend fun cancelAppointment(
        appointmentId: Long,
        cancelReason: String?,
    ): Result<AppointmentApi> {
        return try {
            Log.d("AppointmentRepository", "Отмена записи $appointmentId...")
            val request = if (cancelReason.isNullOrBlank()) null else CancelAppointmentRequest(cancelReason)
            val response = appointmentApi.cancelAppointment(appointmentId, request)

            Log.d("AppointmentRepository", "Response code: ${response.code()}")
            Log.d("AppointmentRepository", "Response isSuccessful: ${response.isSuccessful}")

            if (response.isSuccessful) {
                val appointment = response.body()
                if (appointment != null) {
                    Log.d("AppointmentRepository", "Запись успешно отменена")
                    Result.success(appointment)
                } else {
                    Log.e("AppointmentRepository", "Пустой ответ от сервера")
                    Result.failure(Exception("Пустой ответ от сервера"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("AppointmentRepository", "Error body: $errorBody")
                val errorMessage = errorBody ?: "Ошибка отмены записи: ${response.code()}"
                Log.e("AppointmentRepository", errorMessage)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e("AppointmentRepository", "Исключение при отмене записи", e)
            Result.failure(e)
        }
    }
}
