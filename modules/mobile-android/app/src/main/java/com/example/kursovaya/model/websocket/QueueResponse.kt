package com.example.kursovaya.model.websocket

import com.google.gson.annotations.SerializedName

data class QueueResponse(
    val success: Boolean,
    val message: String,
    val data: Any?, // Can be List<QueueEntry>, PositionData, or null
)

data class QueueEntry(
    val id: Long? = null,
    @SerializedName("doctorId")
    val doctorId: Long,
    @SerializedName("appointmentId")
    val appointmentId: Long? = null,
    @SerializedName("patientId")
    val patientId: Long,
    val position: Int,
    @SerializedName("lastUpdated")
    val lastUpdated: String,
)

data class QueueUpdate(
    @SerializedName("doctorId")
    val doctorId: Long,
    val queue: List<QueueEntry>,
)

data class PositionData(
    @SerializedName("queueEntryId")
    val queueEntryId: Long,
    @SerializedName("doctorId")
    val doctorId: Long,
    @SerializedName("patientId")
    val patientId: Long,
    val position: Int,
    @SerializedName("isNext")
    val isNext: Boolean,
    val message: String,
)
