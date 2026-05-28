package com.example.kursovaya.model

// Основной класс для профиля доктора
data class DoctorProfile(
    val id: String,
    val name: String,
    val specialty: String,
    val rating: Double,
    val reviewCount: Int,
    val experience: String,
    val location: String,
    val availability: String,
    val image: String,
    val consultationFee: String,
    val about: String,
    val education: List<Education>,
    val reviews: List<com.example.kursovaya.model.Review>,
)

data class Review(
    val id: Int? = null,
    val authorName: String,
    val rating: Int,
    val relativeTimeDescription: String,
    val text: String,
)

// Вспомогательные классы для вложенных данных
data class Education(
    val degree: String,
    val institution: String,
    val year: String,
)
