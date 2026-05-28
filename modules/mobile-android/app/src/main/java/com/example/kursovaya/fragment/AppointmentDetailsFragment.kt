package com.example.kursovaya.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.kursovaya.R
import com.example.kursovaya.model.Appointment
import com.example.kursovaya.model.AppointmentStatus
import com.example.kursovaya.repository.AppointmentRepository
import com.example.kursovaya.repository.ReviewRepository
import com.example.kursovaya.repository.UserDataRepository
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AppointmentDetailsFragment : Fragment() {
    private val args: AppointmentDetailsFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_appointment_details, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        val appointment = args.appointment

        val backButton: ImageButton = view.findViewById(R.id.backButton)
        backButton.setOnClickListener { findNavController().navigateUp() }

        // Set status bar color to white
        activity?.window?.statusBarColor = ContextCompat.getColor(requireContext(), R.color.white)

        bindAppointmentData(view, appointment)
    }

    private fun bindAppointmentData(
        view: View,
        appointment: Appointment,
    ) {
        // Doctor info
        val doctorImageView: ImageView = view.findViewById(R.id.doctorImageView)
        val doctorPlaceholderIcon: ImageView = view.findViewById(R.id.doctorPlaceholderIcon)
        val doctorNameTextView: TextView = view.findViewById(R.id.doctorNameTextView)
        val doctorSpecialtyTextView: TextView = view.findViewById(R.id.doctorSpecialtyTextView)
        val doctorRatingBar: RatingBar = view.findViewById(R.id.doctorRatingBar)
        val ratingTextView: TextView = view.findViewById(R.id.ratingTextView)
        val experienceTextView: TextView = view.findViewById(R.id.experienceTextView)
        val statusTextView: TextView = view.findViewById(R.id.statusTextView)

        // Appointment info
        val appointmentDateTextView: TextView = view.findViewById(R.id.appointmentDateTextView)
        val appointmentTimeTextView: TextView = view.findViewById(R.id.appointmentTimeTextView)
        val appointmentRoomTextView: TextView = view.findViewById(R.id.appointmentRoomTextView)
        val appointmentServiceRow: LinearLayout = view.findViewById(R.id.appointmentServiceRow)
        val appointmentServiceTextView: TextView = view.findViewById(R.id.appointmentServiceTextView)

        // Contact info
        val phoneLayout: LinearLayout = view.findViewById(R.id.phoneLayout)
        val emailLayout: LinearLayout = view.findViewById(R.id.emailLayout)
        val doctorPhoneTextView: TextView = view.findViewById(R.id.doctorPhoneTextView)
        val doctorEmailTextView: TextView = view.findViewById(R.id.doctorEmailTextView)

        // Diagnosis
        val diagnosisCard: MaterialCardView = view.findViewById(R.id.diagnosisCard)
        val diagnosisTextView: TextView = view.findViewById(R.id.diagnosisTextView)

        // Cancel reason
        val cancelReasonCard: MaterialCardView = view.findViewById(R.id.cancelReasonCard)
        val cancelReasonTextView: TextView = view.findViewById(R.id.cancelReasonTextView)

        // Buttons
        val upcomingButtonsLayout: LinearLayout = view.findViewById(R.id.upcomingButtonsLayout)
        val rateAndReviewButton: MaterialButton = view.findViewById(R.id.rateAndReviewButton)

        Log.d("AppointmentDetailsFragment", "Appointment: $appointment")
        // Load doctor image
        if (!appointment.image.isNullOrEmpty()) {
            Glide.with(this)
                .load(appointment.image)
                .circleCrop()
                .into(doctorImageView)
            doctorPlaceholderIcon.visibility = View.GONE
        } else {
            doctorImageView.setImageDrawable(
                ContextCompat.getDrawable(requireContext(), R.drawable.bg_doctor_avatar_large),
            )
            doctorPlaceholderIcon.visibility = View.VISIBLE
        }

        // Bind doctor data
        doctorNameTextView.text = appointment.doctorName
        doctorSpecialtyTextView.text = appointment.specialty
        doctorRatingBar.rating = appointment.rating
        ratingTextView.text =
            String.format(
                Locale.getDefault(),
                "%.1f (%d отзывов)",
                appointment.rating,
                appointment.reviewCount,
            )

        // Experience
        if (appointment.experienceYears > 0) {
            experienceTextView.visibility = View.VISIBLE
            experienceTextView.text = getExperienceText(appointment.experienceYears)
        } else {
            experienceTextView.visibility = View.GONE
        }

        // Status
        bindStatus(statusTextView, appointment.status)

        // Date and time
        appointmentDateTextView.text =
            SimpleDateFormat("dd MMM yyyy", Locale("ru", "RU"))
                .format(appointment.date)

        val timeText =
            if (appointment.endTime != null) {
                "${appointment.time} - ${appointment.endTime}"
            } else {
                appointment.time
            }
        appointmentTimeTextView.text = timeText

        // Room
        appointmentRoomTextView.text = "№${appointment.roomCode} - ${appointment.roomName}"

        val svcName = appointment.serviceName?.trim()
        if (!svcName.isNullOrEmpty()) {
            appointmentServiceRow.visibility = View.VISIBLE
            val price = appointment.servicePriceDisplay
            appointmentServiceTextView.text =
                if (!price.isNullOrEmpty()) {
                    getString(R.string.appointment_details_service_with_price, svcName, price)
                } else {
                    getString(R.string.appointment_details_service_no_price, svcName)
                }
        } else {
            appointmentServiceRow.visibility = View.GONE
        }

        // Contact info
        if (!appointment.phone.isNullOrEmpty()) {
            doctorPhoneTextView.text = appointment.phone
            phoneLayout.setOnClickListener {
                val intent =
                    Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:${appointment.phone}")
                    }
                startActivity(intent)
            }
        } else {
            phoneLayout.visibility = View.GONE
        }

        if (!appointment.email.isNullOrEmpty()) {
            doctorEmailTextView.text = appointment.email
            emailLayout.setOnClickListener {
                val intent =
                    Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:${appointment.email}")
                    }
                startActivity(intent)
            }
        } else {
            emailLayout.visibility = View.GONE
        }

        // Diagnosis (show only for completed appointments with diagnosis)
        if (appointment.status == AppointmentStatus.COMPLETED && !appointment.diagnosis.isNullOrEmpty()) {
            diagnosisCard.visibility = View.VISIBLE
            diagnosisTextView.text = appointment.diagnosis
        } else {
            diagnosisCard.visibility = View.GONE
        }

        // Cancel reason (show only for cancelled appointments)
        if (appointment.status == AppointmentStatus.CANCELLED && !appointment.cancelReason.isNullOrEmpty()) {
            cancelReasonCard.visibility = View.VISIBLE
            cancelReasonTextView.text = appointment.cancelReason
        } else {
            cancelReasonCard.visibility = View.GONE
        }

        // Action buttons based on status
        when (appointment.status) {
            AppointmentStatus.UPCOMING -> {
                upcomingButtonsLayout.visibility = View.VISIBLE
                rateAndReviewButton.visibility = View.GONE
            }

            AppointmentStatus.COMPLETED -> {
                upcomingButtonsLayout.visibility = View.GONE
                rateAndReviewButton.visibility = View.VISIBLE
            }

            else -> {
                upcomingButtonsLayout.visibility = View.GONE
                rateAndReviewButton.visibility = View.GONE
            }
        }

        // Кнопка навигации до кабинета
        val navigateButton: MaterialButton = view.findViewById(R.id.navigateButton)
        navigateButton.setOnClickListener {
            navigateToRoom(appointment)
        }

        // Кнопка отмены записи
        val cancelButton: MaterialButton = view.findViewById(R.id.cancelButton)
        cancelButton.setOnClickListener {
            showCancelDrawer(appointment)
        }

        rateAndReviewButton.setOnClickListener {
            showReviewDrawer(appointment)
        }
    }

    private fun navigateToRoom(appointment: Appointment) {
        Log.d("AppointmentDetails", "=== Навигация на карту ===")
        Log.d("AppointmentDetails", "ID записи: ${appointment.id}")
        Log.d("AppointmentDetails", "Врач: ${appointment.doctorName}")
        Log.d("AppointmentDetails", "Специальность: ${appointment.specialty}")
        Log.d("AppointmentDetails", "Код кабинета (roomCode): ${appointment.roomCode}")
        Log.d("AppointmentDetails", "Название кабинета (roomName): ${appointment.roomName}")
        Log.d("AppointmentDetails", "Дата: ${appointment.date}")
        Log.d("AppointmentDetails", "Время: ${appointment.time}")
        Log.d("AppointmentDetails", "==========================")

        val bundle =
            Bundle().apply {
                putString("roomId", appointment.roomCode)
            }
        findNavController().navigate(R.id.nav_map, bundle)
    }

    private fun bindStatus(
        statusTextView: TextView,
        status: AppointmentStatus,
    ) {
        when (status) {
            AppointmentStatus.UPCOMING -> {
                statusTextView.text = "Запланировано"
                statusTextView.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.in_progress_status,
                    ),
                )
                statusTextView.setBackgroundResource(R.drawable.bg_status_scheduled)
            }

            AppointmentStatus.COMPLETED -> {
                statusTextView.text = "Завершено"
                statusTextView.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.completed_status,
                    ),
                )
                statusTextView.setBackgroundResource(R.drawable.bg_status_completed)
            }

            AppointmentStatus.CANCELLED -> {
                statusTextView.text = "Отменено"
                statusTextView.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.cancelled_status,
                    ),
                )
                statusTextView.setBackgroundResource(R.drawable.bg_status_cancelled)
            }
        }
    }

    private fun getExperienceText(years: Int): String {
        val lastDigit = years % 10
        val lastTwoDigits = years % 100

        val yearWord =
            when {
                lastTwoDigits in 11..14 -> "лет"
                lastDigit == 1 -> "год"
                lastDigit in 2..4 -> "года"
                else -> "лет"
            }

        return "Стаж $years $yearWord"
    }

    private fun showReviewDrawer(appointment: Appointment) {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.drawer_review, null)
        bottomSheetDialog.setContentView(view)

        val doctorImageView: ImageView = view.findViewById(R.id.doctorImageView)
        val doctorPlaceholderIcon: ImageView = view.findViewById(R.id.doctorPlaceholderIcon)
        val doctorNameTextView: TextView = view.findViewById(R.id.doctorNameTextView)
        val doctorSpecialtyTextView: TextView = view.findViewById(R.id.doctorSpecialtyTextView)
        val ratingBar: RatingBar = view.findViewById(R.id.ratingBar)
        val commentEditText: TextInputEditText = view.findViewById(R.id.commentEditText)
        val submitButton: MaterialButton = view.findViewById(R.id.submitReviewButton)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)

        // Load doctor image
        if (!appointment.image.isNullOrEmpty()) {
            Glide.with(this)
                .load(appointment.image)
                .circleCrop()
                .into(doctorImageView)
            doctorPlaceholderIcon.visibility = View.GONE
        } else {
            doctorImageView.setImageDrawable(
                ContextCompat.getDrawable(requireContext(), R.drawable.bg_doctor_avatar_large),
            )
            doctorPlaceholderIcon.visibility = View.VISIBLE
        }

        doctorNameTextView.text = appointment.doctorName
        doctorSpecialtyTextView.text = appointment.specialty

        val reviewRepository = ReviewRepository(requireContext())
        val appointmentId = appointment.id.toLongOrNull() ?: 0L
        var existingReviewId: Long? = null

        // Check if review already exists
        progressBar.visibility = View.VISIBLE
        submitButton.isEnabled = false

        lifecycleScope.launch {
            val reviewResult = reviewRepository.getReviewByAppointmentId(appointmentId)

            reviewResult.onSuccess { existingReview ->
                if (existingReview != null) {
                    // Fill in existing review data
                    existingReviewId = existingReview.id
                    ratingBar.rating = existingReview.rating.toFloat()
                    if (!existingReview.reviewText.isNullOrEmpty()) {
                        commentEditText.setText(existingReview.reviewText)
                    }
                    submitButton.text = "Обновить отзыв"
                    Log.d(
                        "AppointmentDetailsFragment",
                        "Загружен существующий отзыв: id=${existingReview.id}, rating=${existingReview.rating}",
                    )
                } else {
                    // No review exists, reset to defaults
                    existingReviewId = null
                    ratingBar.rating = 5f
                    commentEditText.setText("")
                    submitButton.text = "Отправить отзыв"
                    Log.d("AppointmentDetailsFragment", "Отзыв не найден, используем значения по умолчанию")
                }
                progressBar.visibility = View.GONE
                submitButton.isEnabled = true
            }.onFailure { error ->
                Log.e("AppointmentDetailsFragment", "Ошибка загрузки отзыва", error)
                // В случае ошибки все равно показываем drawer с пустыми полями
                existingReviewId = null
                ratingBar.rating = 5f
                commentEditText.setText("")
                submitButton.text = "Отправить отзыв"
                progressBar.visibility = View.GONE
                submitButton.isEnabled = true
            }
        }

        submitButton.setOnClickListener {
            val rating = ratingBar.rating.toInt()
            val comment = commentEditText.text.toString().trim()

            if (rating == 0) {
                Toast.makeText(
                    requireContext(),
                    "Пожалуйста, выберите оценку",
                    Toast.LENGTH_SHORT,
                ).show()
                return@setOnClickListener
            }

            // Get patient ID
            val user = UserDataRepository.getCurrentUser()
            val patientId = user?.patientId

            if (patientId == null) {
                Toast.makeText(
                    requireContext(),
                    "Ошибка: не найден ID пациента",
                    Toast.LENGTH_SHORT,
                ).show()
                return@setOnClickListener
            }

            // Disable button and show progress
            submitButton.isEnabled = false
            progressBar.visibility = View.VISIBLE

            lifecycleScope.launch {
                val result =
                    if (existingReviewId != null) {
                        // Update existing review
                        reviewRepository.updateReview(
                            reviewId = existingReviewId!!,
                            rating = rating,
                            reviewText = if (comment.isNotEmpty()) comment else null,
                        )
                    } else {
                        // Create new review
                        reviewRepository.createReview(
                            appointmentId = appointment.id.toLongOrNull() ?: 0L,
                            doctorId = appointment.doctorId.toLongOrNull() ?: 0L,
                            patientId = patientId,
                            rating = rating,
                            reviewText = if (comment.isNotEmpty()) comment else null,
                        )
                    }

                result.onSuccess {
                    val message =
                        if (existingReviewId != null) {
                            "Отзыв успешно обновлен!"
                        } else {
                            "Отзыв успешно отправлен!"
                        }
                    Toast.makeText(
                        requireContext(),
                        message,
                        Toast.LENGTH_SHORT,
                    ).show()
                    bottomSheetDialog.dismiss()
                    // Можно обновить данные записи или вернуться назад
                    findNavController().navigateUp()
                }.onFailure { error ->
                    Log.e("AppointmentDetailsFragment", "Ошибка отправки отзыва", error)
                    val action = if (existingReviewId != null) "обновления" else "отправки"
                    Toast.makeText(
                        requireContext(),
                        "Ошибка $action отзыва: ${error.message}",
                        Toast.LENGTH_LONG,
                    ).show()
                    submitButton.isEnabled = true
                    progressBar.visibility = View.GONE
                }
            }
        }

        bottomSheetDialog.show()
    }

    private fun showCancelDrawer(appointment: Appointment) {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.drawer_cancel_appointment, null)
        bottomSheetDialog.setContentView(view)

        val chipGroupReasons: ChipGroup = view.findViewById(R.id.chipGroupReasons)
        val reasonEditText: TextInputEditText = view.findViewById(R.id.reasonEditText)
        val cancelDialogButton: MaterialButton = view.findViewById(R.id.cancelDialogButton)
        val confirmCancelButton: MaterialButton = view.findViewById(R.id.confirmCancelButton)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)

        var selectedReason: String? = null

        // Обработка выбора чипа
        chipGroupReasons.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val checkedChip = group.findViewById<Chip>(checkedIds[0])
                selectedReason = checkedChip?.text?.toString()
                // Очищаем текстовое поле при выборе готовой причины
                reasonEditText.setText("")
            } else {
                selectedReason = null
            }
        }

        // Закрыть drawer
        cancelDialogButton.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        // Подтвердить отмену
        confirmCancelButton.setOnClickListener {
            val customReason = reasonEditText.text.toString().trim()
            val finalReason =
                when {
                    customReason.isNotEmpty() -> customReason
                    selectedReason != null -> selectedReason
                    else -> null
                }

            // Disable buttons and show progress
            confirmCancelButton.isEnabled = false
            cancelDialogButton.isEnabled = false
            progressBar.visibility = View.VISIBLE

            val appointmentRepository = AppointmentRepository(requireContext())
            val appointmentId = appointment.id.toLongOrNull() ?: 0L

            lifecycleScope.launch {
                appointmentRepository.cancelAppointment(appointmentId, finalReason)
                    .onSuccess {
                        Toast.makeText(
                            requireContext(),
                            "Запись успешно отменена",
                            Toast.LENGTH_SHORT,
                        ).show()
                        bottomSheetDialog.dismiss()
                        findNavController().navigateUp()
                    }
                    .onFailure { error ->
                        Log.e("AppointmentDetailsFragment", "Ошибка отмены записи", error)
                        Toast.makeText(
                            requireContext(),
                            "Ошибка отмены записи: ${error.message}",
                            Toast.LENGTH_LONG,
                        ).show()
                        confirmCancelButton.isEnabled = true
                        cancelDialogButton.isEnabled = true
                        progressBar.visibility = View.GONE
                    }
            }
        }

        bottomSheetDialog.show()
    }
}
