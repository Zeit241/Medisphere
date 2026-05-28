package com.example.kursovaya.model

data class Doctor(
    val id: String,
    val name: String,
    val specialty: String,
    val rating: Double,
    val reviews: Int,
    val experience: String,
    val location: String,
    val availability: String,
    val image: String, // URL or resource ID
    val consultationFee: String,
)
