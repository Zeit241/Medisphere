package com.example.kursovaya.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kursovaya.R
import com.example.kursovaya.model.Appointment
import com.example.kursovaya.model.AppointmentStatus
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.*

class AppointmentAdapter(
    private var appointments: List<Appointment>,
    private val onAppointmentClick: (Appointment) -> Unit,
    private val onNavigateClick: ((Appointment) -> Unit)? = null,
    private val onCancelClick: ((Appointment) -> Unit)? = null,
    private val onRepeatClick: ((Appointment) -> Unit)? = null,
) : RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder>() {
    private var showStatus: Boolean = false

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): AppointmentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_appointment, parent, false)
        return AppointmentViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: AppointmentViewHolder,
        position: Int,
    ) {
        holder.bind(appointments[position], showStatus)
    }

    override fun getItemCount(): Int = appointments.size

    fun updateAppointments(
        newAppointments: List<Appointment>,
        showStatus: Boolean = false,
    ) {
        appointments = newAppointments
        this.showStatus = showStatus
        notifyDataSetChanged()
    }

    inner class AppointmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val doctorImageView: ImageView = itemView.findViewById(R.id.doctorImageView)
        private val doctorPlaceholderIcon: ImageView = itemView.findViewById(R.id.doctorPlaceholderIcon)
        private val doctorNameTextView: TextView = itemView.findViewById(R.id.doctorNameTextView)
        private val doctorSpecialtyTextView: TextView = itemView.findViewById(R.id.doctorSpecialtyTextView)
        private val statusChip: Chip = itemView.findViewById(R.id.statusChip)
        private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        private val timeTextView: TextView = itemView.findViewById(R.id.timeTextView)
        private val serviceLineTextView: TextView = itemView.findViewById(R.id.serviceLineTextView)
        private val upcomingButtonsLayout: LinearLayout = itemView.findViewById(R.id.upcomingButtonsLayout)
        private val repeatButton: Button = itemView.findViewById(R.id.repeatButton)
        private val navigateButton: Button = itemView.findViewById(R.id.navigateButton)
        private val cancelButton: Button = itemView.findViewById(R.id.cancelButton)

        fun bind(
            appointment: Appointment,
            showStatus: Boolean,
        ) {
            itemView.setOnClickListener { onAppointmentClick(appointment) }

            // Обработчик кнопки навигации
            navigateButton.setOnClickListener {
                onNavigateClick?.invoke(appointment)
            }

            // Обработчик кнопки отмены
            cancelButton.setOnClickListener {
                onCancelClick?.invoke(appointment)
            }

            // Обработчик кнопки повторить
            repeatButton.setOnClickListener {
                onRepeatClick?.invoke(appointment)
            }

            // Bind data to views
            doctorNameTextView.text = appointment.doctorName
            doctorSpecialtyTextView.text = appointment.specialty
            dateTextView.text = SimpleDateFormat("dd MMM yyyy", Locale("ru", "RU")).format(appointment.date)
            timeTextView.text = appointment.time

            val svcName = appointment.serviceName?.trim()
            if (!svcName.isNullOrEmpty()) {
                serviceLineTextView.visibility = View.VISIBLE
                val price = appointment.servicePriceDisplay
                serviceLineTextView.text =
                    if (!price.isNullOrEmpty()) {
                        itemView.context.getString(
                            R.string.appointment_list_service_with_price,
                            svcName,
                            price,
                        )
                    } else {
                        itemView.context.getString(
                            R.string.appointment_list_service_no_price,
                            svcName,
                        )
                    }
            } else {
                serviceLineTextView.visibility = View.GONE
            }

            // Load doctor image or show placeholder
            if (!appointment.image.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(appointment.image)
                    .placeholder(R.drawable.bg_doctor_placeholder)
                    .error(R.drawable.bg_doctor_placeholder)
                    .centerCrop()
                    .into(doctorImageView)
                doctorPlaceholderIcon.visibility = View.GONE
            } else {
                doctorImageView.setImageResource(R.drawable.bg_doctor_placeholder)
                doctorPlaceholderIcon.visibility = View.VISIBLE
            }

            // Show/hide status chip based on current tab
            if (showStatus) {
                statusChip.visibility = View.VISIBLE
                statusChip.text = appointment.status.value
                // Set status chip color
                when (appointment.status) {
                    AppointmentStatus.UPCOMING -> statusChip.setChipBackgroundColorResource(R.color.upcoming_status)
                    AppointmentStatus.COMPLETED -> statusChip.setChipBackgroundColorResource(R.color.completed_status)
                    AppointmentStatus.CANCELLED -> statusChip.setChipBackgroundColorResource(R.color.cancelled_status)
                }
            } else {
                statusChip.visibility = View.GONE
            }

            // Show/hide buttons based on status
            when (appointment.status) {
                AppointmentStatus.UPCOMING -> {
                    upcomingButtonsLayout.visibility = View.VISIBLE
                    repeatButton.visibility = View.GONE
                }
                AppointmentStatus.COMPLETED, AppointmentStatus.CANCELLED -> {
                    upcomingButtonsLayout.visibility = View.GONE
                    repeatButton.visibility = View.VISIBLE
                }
            }
        }
    }
}
