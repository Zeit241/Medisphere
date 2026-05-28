package com.example.kursovaya.model

data class QueueItem(
    val id: String,
    val doctorName: String,
    val specialty: String,
    val currentNumber: Int,
    val yourNumber: Int,
    val estimatedWait: String,
    val status: String, // "waiting", "next", "ready"
    val peopleAhead: Int,
    val image: String,
)
