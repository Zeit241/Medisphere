package com.example.kursovaya.model.api

import com.google.gson.annotations.SerializedName

data class DashboardResponse(
    @SerializedName("topSpecializations")
    val topSpecializations: List<TopSpecialization>,
    @SerializedName("topDoctors")
    val topDoctors: List<DoctorApi>,
    @SerializedName("upcomingAppointments")
    val upcomingAppointments: List<UpcomingAppointmentApi>,
)

data class TopSpecialization(
    val id: Int,
    val code: String,
    val name: String,
    val description: String?,
    @SerializedName("doctorCount")
    val doctorCount: Int,
)

data class UpcomingAppointmentApi(
    val id: Long,
    @SerializedName("doctorId")
    val doctorId: Long,
    @SerializedName("doctorFirstName")
    val doctorFirstName: String?,
    @SerializedName("doctorLastName")
    val doctorLastName: String?,
    @SerializedName("doctorMiddleName")
    val doctorMiddleName: String?,
    @SerializedName("doctorPhoto")
    val doctorPhoto: String?,
    @SerializedName("doctorSpecializations")
    val doctorSpecializations: List<Specialization>?,
    @SerializedName("roomId")
    val roomId: Long?,
    @SerializedName("roomNumber")
    val roomNumber: String?,
    @SerializedName("startTime")
    val startTime: String,
    @SerializedName("endTime")
    val endTime: String?,
    val status: String,
) {
    /** Формирует полное ФИО: Фамилия Имя Отчество */
    fun getFullName(): String {
        return buildString {
            if (!doctorLastName.isNullOrBlank()) {
                append(doctorLastName)
            }
            if (!doctorFirstName.isNullOrBlank()) {
                if (isNotEmpty()) append(" ")
                append(doctorFirstName)
            }
            if (!doctorMiddleName.isNullOrBlank()) {
                if (isNotEmpty()) append(" ")
                append(doctorMiddleName)
            }
        }.ifBlank { "Врач" }
    }
}
