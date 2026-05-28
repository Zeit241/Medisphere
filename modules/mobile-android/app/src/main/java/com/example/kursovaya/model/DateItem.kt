package com.example.kursovaya.model

data class DateItem(
    val dayOfWeek: String,
    val dayOfMonth: String,
    val month: String,
    val dateString: String, // Формат YYYY-MM-DD для API
    var isSelected: Boolean = false,
)
