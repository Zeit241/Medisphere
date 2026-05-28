package com.example.kursovaya.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class Appointment(
    val id: String,
    val doctorId: String,
    val doctorName: String,
    val specialty: String,
    val date: Date,
    val time: String,
    val endTime: String? = null,
    val roomCode: String,
    val roomName: String,
    val status: AppointmentStatus,
    val image: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val rating: Float = 0f,
    val reviewCount: Int = 0,
    val experienceYears: Int = 0,
    val bio: String? = null,
    val diagnosis: String? = null,
    val cancelReason: String? = null,
    /** Название услуги, если была указана при записи */
    val serviceName: String? = null,
    /** Отформатированная цена (руб.) или null, если цену показывать не нужно / неизвестна */
    val servicePriceDisplay: String? = null,
) : Parcelable
