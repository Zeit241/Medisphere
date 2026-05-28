package com.example.kursovaya.model

data class TimeItem(
    val time: String,
    val appointmentId: Long? = null, // ID записи для бронирования
    val isBooked: Boolean = false, // Занята ли запись
    var isSelected: Boolean = false,
)
