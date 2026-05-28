package com.example.kursovaya.model.api

import com.example.kursovaya.model.Appointment
import com.example.kursovaya.model.AppointmentStatus
import com.example.kursovaya.util.ServicePriceFormatter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Преобразует AppointmentApi в Appointment
 */
fun AppointmentApi.toAppointment(
    doctorName: String,
    doctorSpecialty: String,
    doctorPhone: String?,
    doctorEmail: String?,
    doctorImage: String?,
    doctorRating: Float = 0f,
    doctorReviewCount: Int = 0,
    doctorExperienceYears: Int = 0,
    doctorBio: String? = null,
    roomCode: String = roomId?.toString() ?: "N/A",
    roomName: String = "Кабинет",
): Appointment {
    // Парсим дату и время из ISO формата
    val date = parseIsoDateTime(startTime) ?: Date()
    val time = formatTime(startTime)
    val endTimeFormatted = formatTime(endTime)

    // Преобразуем статус из API в AppointmentStatus
    val appointmentStatus =
        when (status.lowercase()) {
            "scheduled", "upcoming" -> AppointmentStatus.UPCOMING
            "completed" -> AppointmentStatus.COMPLETED
            "cancelled", "canceled" -> AppointmentStatus.CANCELLED
            else -> AppointmentStatus.UPCOMING
        }

    val svc = service
    val svcName = svc?.name?.trim()?.takeIf { it.isNotEmpty() }
    val svcPrice = ServicePriceFormatter.formatRub(svc?.price)

    return Appointment(
        id = id.toString(),
        doctorId = doctorId.toString(),
        doctorName = doctorName,
        specialty = doctorSpecialty,
        date = date,
        time = time,
        endTime = endTimeFormatted,
        roomCode = roomCode,
        roomName = roomName,
        status = appointmentStatus,
        image = doctorImage.toImageDataUri(),
        phone = doctorPhone,
        email = doctorEmail,
        rating = doctorRating,
        reviewCount = doctorReviewCount,
        experienceYears = doctorExperienceYears,
        bio = doctorBio,
        diagnosis = diagnosis,
        cancelReason = cancelReason,
        serviceName = svcName,
        servicePriceDisplay = svcPrice,
    )
}

/**
 * Парсит ISO 8601 дату и время
 */
private fun parseIsoDateTime(dateTimeString: String): Date? {
    val formats =
        listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault()),
        )

    for (format in formats) {
        try {
            return format.parse(dateTimeString)
        } catch (e: Exception) {
            // Пробуем следующий формат
        }
    }
    return null
}

/**
 * Форматирует время из ISO строки в читаемый формат (24-часовой формат)
 */
private fun formatTime(dateTimeString: String): String {
    val date = parseIsoDateTime(dateTimeString)
    return if (date != null) {
        SimpleDateFormat("HH:mm", Locale("ru", "RU")).format(date)
    } else {
        // Если не удалось распарсить, пытаемся извлечь время из строки
        try {
            val timePart = dateTimeString.substringAfter("T").substringBefore(".")
            val parts = timePart.split(":")
            if (parts.size >= 2) {
                val hour = parts[0].toInt()
                val minute = parts[1]
                String.format("%02d:%s", hour, minute)
            } else {
                dateTimeString
            }
        } catch (e: Exception) {
            dateTimeString
        }
    }
}
