package com.example.kursovaya.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.kursovaya.R
import com.example.kursovaya.api.RetrofitClient
import com.example.kursovaya.model.Appointment
import com.example.kursovaya.model.AppointmentStatus
import com.example.kursovaya.model.api.DashboardResponse
import com.example.kursovaya.model.api.DoctorApi
import com.example.kursovaya.model.api.TopSpecialization
import com.example.kursovaya.model.api.UpcomingAppointmentApi
import com.example.kursovaya.model.api.toImageDataUri
import com.example.kursovaya.repository.DashboardRepository
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {
    private lateinit var dashboardRepository: DashboardRepository
    private var progressBar: ProgressBar? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        RetrofitClient.init(requireContext())
        dashboardRepository = DashboardRepository(requireContext())
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        progressBar = view.findViewById(R.id.progressBar)

        setupClickListeners(view)
        loadDashboard(view)
    }

    private fun setupClickListeners(view: View) {
        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigation)

        view.findViewById<MaterialCardView>(R.id.cardBookAppointment).setOnClickListener {
            bottomNav.selectedItemId = R.id.nav_doctors
        }

        view.findViewById<MaterialCardView>(R.id.cardQueueStatus).setOnClickListener {
            bottomNav.selectedItemId = R.id.nav_queue
        }

        view.findViewById<TextView>(R.id.textViewViewAllDoctors).setOnClickListener {
            bottomNav.selectedItemId = R.id.nav_doctors
        }

        view.findViewById<TextView>(R.id.textViewViewAllAppointments).setOnClickListener {
            findNavController().navigate(R.id.appointmentHistoryFragment)
        }
    }

    private fun loadDashboard(view: View) {
        // Проверяем, есть ли кэшированные данные
        val cachedData = dashboardRepository.getCachedDashboard()

        if (cachedData != null) {
            // Отображаем кэшированные данные сразу
            displayDashboard(view, cachedData)

            // Обновляем данные в фоне
            refreshDashboardInBackground(view)
        } else {
            // Нет кэша - показываем лоадер и загружаем
            progressBar?.visibility = View.VISIBLE
            fetchDashboard(view)
        }
    }

    private fun refreshDashboardInBackground(view: View) {
        viewLifecycleOwner.lifecycleScope.launch {
            dashboardRepository.getDashboard(forceRefresh = true)
                .onSuccess { dashboard ->
                    if (!isAdded) return@onSuccess
                    displayDashboard(view, dashboard)
                    Log.d("HomeFragment", "Dashboard refreshed in background")
                }
                .onFailure { error ->
                    // Тихо логируем ошибку, данные уже отображены из кэша
                    Log.e("HomeFragment", "Error refreshing dashboard in background", error)
                }
        }
    }

    private fun fetchDashboard(view: View) {
        viewLifecycleOwner.lifecycleScope.launch {
            dashboardRepository.getDashboard()
                .onSuccess { dashboard ->
                    if (!isAdded) return@onSuccess

                    progressBar?.visibility = View.GONE
                    displayDashboard(view, dashboard)

                    Log.d("HomeFragment", "Dashboard loaded successfully")
                }
                .onFailure { error ->
                    if (!isAdded) return@onFailure

                    progressBar?.visibility = View.GONE
                    Log.e("HomeFragment", "Error loading dashboard", error)

                    Snackbar.make(
                        view,
                        "Ошибка загрузки данных: ${error.message}",
                        Snackbar.LENGTH_LONG,
                    ).setAction("Повторить") {
                        progressBar?.visibility = View.VISIBLE
                        fetchDashboard(view)
                    }.show()
                }
        }
    }

    private fun displayDashboard(
        view: View,
        dashboard: DashboardResponse,
    ) {
        populateSpecialties(view, dashboard.topSpecializations)
        populateDoctorsList(view, dashboard.topDoctors)
        populateUpcomingAppointments(view, dashboard.upcomingAppointments)
    }

    private fun populateSpecialties(
        view: View,
        specializations: List<TopSpecialization>,
    ) {
        val chipGroup = view.findViewById<ChipGroup>(R.id.chipGroupSpecialties)
        chipGroup.removeAllViews()

        for (specialization in specializations) {
            val chip =
                Chip(requireContext()).apply {
                    text = "${specialization.name} (${specialization.doctorCount})"
                    isClickable = true
                    isCheckable = false
                    tag = specialization // Сохраняем данные о специализации
                }

            chip.setOnClickListener {
                navigateToDoctorsWithSpecialization(specialization.name)
            }

            chipGroup.addView(chip)
        }
    }

    private fun navigateToDoctorsWithSpecialization(specializationName: String) {
        // Используем action с аргументом specialization
        val bundle = bundleOf("specialization" to specializationName)
        findNavController().navigate(R.id.action_nav_home_to_doctorsFragment, bundle)
    }

    private fun populateUpcomingAppointments(
        view: View,
        appointments: List<UpcomingAppointmentApi>,
    ) {
        val container = view.findViewById<LinearLayout>(R.id.containerUpcomingAppointments)
        val emptyCard = view.findViewById<MaterialCardView>(R.id.cardNoAppointments)
        val inflater = LayoutInflater.from(requireContext())

        container.removeAllViews()

        // Фильтруем только будущие приемы
        val currentTime = ZonedDateTime.now()
        val futureAppointments =
            appointments.filter { appointment ->
                try {
                    val appointmentTime = ZonedDateTime.parse(appointment.startTime)
                    appointmentTime.isAfter(currentTime)
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Error parsing appointment time: ${appointment.startTime}", e)
                    false
                }
            }

        if (futureAppointments.isEmpty()) {
            emptyCard.visibility = View.VISIBLE
            container.visibility = View.GONE
            return
        }

        emptyCard.visibility = View.GONE
        container.visibility = View.VISIBLE

        for (appointmentApi in futureAppointments) {
            val cardView = inflater.inflate(R.layout.item_upcoming_appointment, container, false)

            val doctorNameTextView = cardView.findViewById<TextView>(R.id.textDoctorName)
            val specialtyTextView = cardView.findViewById<TextView>(R.id.textSpecialty)
            val dateTimeTextView = cardView.findViewById<TextView>(R.id.textDateTime)
            val roomTextView = cardView.findViewById<TextView>(R.id.textRoom)
            val doctorPhotoImageView = cardView.findViewById<ImageView>(R.id.imageDoctorPhoto)

            // Формируем полное ФИО: Фамилия Имя Отчество
            doctorNameTextView.text = appointmentApi.getFullName()

            // Специальность из doctorSpecializations
            val specialty = appointmentApi.doctorSpecializations?.firstOrNull()?.name ?: "Врач"
            specialtyTextView.text = specialty

            // Форматируем дату и время
            val formattedDateTime = formatAppointmentTime(appointmentApi.startTime)
            dateTimeTextView.text = formattedDateTime

            // Номер кабинета (если есть view для него)
            roomTextView?.let {
                if (appointmentApi.roomNumber != null) {
                    it.text = appointmentApi.roomNumber
                    it.visibility = View.VISIBLE
                } else {
                    it.visibility = View.GONE
                }
            }

            val photoLoad = appointmentApi.doctorPhoto?.toImageDataUri()?.takeIf { it.isNotEmpty() }
            if (!photoLoad.isNullOrBlank()) {
                Glide.with(this@HomeFragment)
                    .load(photoLoad)
                    .placeholder(R.drawable.placeholder_doctor)
                    .error(R.drawable.placeholder_doctor)
                    .circleCrop()
                    .into(doctorPhotoImageView)
            } else {
                doctorPhotoImageView.setImageResource(R.drawable.placeholder_doctor)
            }

            // Клик на карточку - переход к деталям приёма
            cardView.setOnClickListener {
                navigateToAppointmentDetails(appointmentApi)
            }

            container.addView(cardView)
        }
    }

    private fun navigateToAppointmentDetails(appointmentApi: UpcomingAppointmentApi) {
        // Конвертируем UpcomingAppointmentApi в Appointment для передачи в AppointmentDetailsFragment
        val appointment = appointmentApi.toAppointment()
        val bundle = bundleOf("appointment" to appointment)
        findNavController().navigate(R.id.action_nav_home_to_appointmentDetailsFragment, bundle)
    }

    private fun UpcomingAppointmentApi.toAppointment(): Appointment {
        // Парсим дату и время
        val (date, time, endTimeStr) = parseDateTime(startTime, endTime)

        // Специальность
        val specialty = doctorSpecializations?.firstOrNull()?.name ?: "Врач"

        // Код и название кабинета
        val roomCode = roomId?.toString() ?: ""
        val roomName = roomNumber ?: "Кабинет"

        // Статус
        val appointmentStatus =
            when (status.lowercase()) {
                "scheduled", "upcoming" -> AppointmentStatus.UPCOMING
                "completed" -> AppointmentStatus.COMPLETED
                "cancelled", "canceled" -> AppointmentStatus.CANCELLED
                else -> AppointmentStatus.UPCOMING
            }

        val photoUrl = doctorPhoto?.toImageDataUri()?.takeIf { it.isNotEmpty() }

        return Appointment(
            id = id.toString(),
            doctorId = doctorId.toString(),
            doctorName = getFullName(),
            specialty = specialty,
            date = date,
            time = time,
            endTime = endTimeStr,
            roomCode = roomCode,
            roomName = roomName,
            status = appointmentStatus,
            image = photoUrl,
        )
    }

    private fun parseDateTime(
        startTimeStr: String,
        endTimeStr: String?,
    ): Triple<Date, String, String?> {
        return try {
            val startZoned = ZonedDateTime.parse(startTimeStr)
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

            val date = Date.from(startZoned.toInstant())
            val time = startZoned.format(timeFormatter)

            val endTime =
                endTimeStr?.let {
                    try {
                        val endZoned = ZonedDateTime.parse(it)
                        endZoned.format(timeFormatter)
                    } catch (e: Exception) {
                        null
                    }
                }

            Triple(date, time, endTime)
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error parsing date: $startTimeStr", e)
            Triple(Date(), "00:00", null)
        }
    }

    private fun formatAppointmentTime(isoDateTime: String): String {
        return try {
            val zonedDateTime = ZonedDateTime.parse(isoDateTime)
            val formatter = DateTimeFormatter.ofPattern("d MMMM, HH:mm", Locale("ru"))
            zonedDateTime.format(formatter)
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error parsing date: $isoDateTime", e)
            isoDateTime
        }
    }

    private fun populateDoctorsList(
        view: View,
        doctors: List<DoctorApi>,
    ) {
        val doctorsContainer = view.findViewById<LinearLayout>(R.id.containerDoctors)
        val inflater = LayoutInflater.from(requireContext())

        doctorsContainer.removeAllViews()

        for (doctorApi in doctors) {
            val doctorCardView = inflater.inflate(R.layout.list_item_doctor, doctorsContainer, false)

            val nameTextView = doctorCardView.findViewById<TextView>(R.id.doctorNameTextView)
            val specialtyTextView = doctorCardView.findViewById<TextView>(R.id.doctorSpecialtyTextView)
            val ratingTextView = doctorCardView.findViewById<TextView>(R.id.doctorRatingTextView)
            val reviewsTextView = doctorCardView.findViewById<TextView>(R.id.doctorReviewsTextView)
            val bookButton = doctorCardView.findViewById<ImageView>(R.id.bookButton)
            val doctorImageView = doctorCardView.findViewById<ImageView>(R.id.doctorImageView)

            // Формируем полное имя
            val fullName =
                buildString {
                    append(doctorApi.user.lastName)
                    if (doctorApi.user.firstName.isNotEmpty()) {
                        append(" ${doctorApi.user.firstName}")
                    }
                    if (doctorApi.user.middleName?.isNotEmpty() == true) {
                        append(" ${doctorApi.user.middleName}")
                    }
                }.trim()

            nameTextView.text = fullName.ifEmpty { doctorApi.user.email }

            // Формируем специальность
            val specialtyText =
                if (!doctorApi.specializations.isNullOrEmpty()) {
                    doctorApi.specializations.joinToString(", ") { it.name }
                } else {
                    doctorApi.bio ?: "Врач"
                }
            specialtyTextView.text = specialtyText

            ratingTextView.text = String.format(Locale.US, "%.1f", doctorApi.rating ?: 0.0)
            reviewsTextView.text = "(${doctorApi.reviewCount ?: 0})"

            val homeDoctorPhoto = doctorApi.photoUrl?.toImageDataUri()?.takeIf { it.isNotEmpty() }
            if (!homeDoctorPhoto.isNullOrBlank()) {
                Glide.with(requireContext())
                    .load(homeDoctorPhoto)
                    .placeholder(R.drawable.placeholder_doctor)
                    .error(R.drawable.placeholder_doctor)
                    .circleCrop()
                    .into(doctorImageView)
            } else {
                doctorImageView.setImageResource(R.drawable.placeholder_doctor)
            }

            // Клик на карточку - переход к профилю врача
            doctorCardView.setOnClickListener {
                val bundle = bundleOf("DOCTOR_ID" to doctorApi.id.toString())
                findNavController().navigate(R.id.action_nav_home_to_doctorProfileFragment, bundle)
            }

            // Клик на иконку записи - переход на страницу букинга
            bookButton.setOnClickListener {
                val bundle = bundleOf("DOCTOR_ID" to doctorApi.id.toString())
                findNavController().navigate(R.id.action_nav_home_to_bookingFragment, bundle)
            }

            doctorsContainer.addView(doctorCardView)
        }
    }
}
