package com.example.kursovaya.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.kursovaya.R
import com.example.kursovaya.adapter.DateAdapter
import com.example.kursovaya.adapter.TimeAdapter
import com.example.kursovaya.databinding.FragmentBookingBinding
import com.example.kursovaya.model.DateItem
import com.example.kursovaya.model.Doctor
import com.example.kursovaya.model.TimeItem
import com.example.kursovaya.model.api.ClinicServiceItem
import com.example.kursovaya.model.api.toImageDataUri
import com.example.kursovaya.repository.AppointmentRepository
import com.example.kursovaya.repository.CatalogRepository
import com.example.kursovaya.repository.DoctorsRepository
import com.example.kursovaya.repository.UserDataRepository
import com.example.kursovaya.util.ServicePriceFormatter
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class BookingFragment : Fragment() {
    private var _binding: FragmentBookingBinding? = null
    private val binding get() = _binding!!

    private lateinit var dateAdapter: DateAdapter
    private lateinit var timeAdapter: TimeAdapter

    private var selectedDate: DateItem? = null
    private var selectedTime: TimeItem? = null
    private var currentDoctor: Doctor? = null
    private var doctorId: Long? = null
    private var selectedServiceId: Long? = null
    private var doctorServicesList: List<ClinicServiceItem> = emptyList()

    private lateinit var appointmentRepository: AppointmentRepository
    private lateinit var doctorsRepository: DoctorsRepository
    private lateinit var catalogRepository: CatalogRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentBookingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        appointmentRepository = AppointmentRepository(requireContext())
        doctorsRepository = DoctorsRepository(requireContext())
        catalogRepository = CatalogRepository(requireContext())

        setupToolbar()
        setupDateRecyclerView()
        setupTimeRecyclerView()

        val doctorIdString = arguments?.getString("DOCTOR_ID")
        doctorId = doctorIdString?.toLongOrNull()

        if (doctorId != null) {
            loadDoctorInfo(doctorId!!)
            setupServiceSelector(doctorId!!)
        } else {
            Toast.makeText(requireContext(), "Ошибка: ID врача не указан", Toast.LENGTH_SHORT)
                .show()
            findNavController().navigateUp()
        }

        binding.confirmBookingButton.setOnClickListener {
            confirmBooking()
        }

        binding.backToHomeButton.setOnClickListener {
            findNavController().popBackStack(R.id.nav_home, false)
        }

        binding.viewAppointmentsButton.setOnClickListener {
            findNavController().navigate(R.id.appointmentHistoryFragment)
        }
    }

    private fun setupServiceSelector(doctorId: Long) {
        val preFromArgs = arguments?.getString("SERVICE_ID")?.toLongOrNull()
        lifecycleScope.launch {
            val result = catalogRepository.getServicesForDoctor(doctorId)
            result.onSuccess { list ->
                if (_binding == null) return@onSuccess
                doctorServicesList = list
                val noneLabel = getString(R.string.booking_service_none)
                val options = mutableListOf<Pair<Long?, String>>()
                options.add(null to noneLabel)
                list.filter { it.id != null && !it.name.isNullOrBlank() }.forEach { item ->
                    options.add(item.id to item.name!!)
                }
                val labels = options.map { it.second }
                val ddAdapter =
                    ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        labels,
                    )
                binding.serviceAutoComplete.setAdapter(ddAdapter)

                val idx =
                    when {
                        preFromArgs != null -> {
                            val i = options.indexOfFirst { it.first == preFromArgs }
                            if (i >= 0) i else 0
                        }
                        else -> 0
                    }
                selectedServiceId = options[idx].first
                binding.serviceAutoComplete.setText(labels[idx], false)

                binding.serviceInputLayout.isVisible = options.size > 1
                if (options.size <= 1) return@onSuccess

                binding.serviceAutoComplete.setOnItemClickListener { _, _, position, _ ->
                    selectedServiceId = options[position].first
                    selectedDate?.dateString?.let { loadAvailableTimes(it) }
                }

                selectedDate?.dateString?.let { loadAvailableTimes(it) }
            }
            result.onFailure {
                if (_binding == null) return@onFailure
                doctorServicesList = emptyList()
                binding.serviceInputLayout.isVisible = false
                selectedServiceId = null
            }
        }
    }

    private fun setupToolbar() {
        (activity as? AppCompatActivity)?.setSupportActionBar(binding.toolbar)
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            if (binding.bookingConfirmationContainer.visibility == View.VISIBLE) {
                showSelectionScreen()
            } else {
                findNavController().navigateUp()
            }
        }
    }

    private fun loadDoctorInfo(doctorId: Long) {
        lifecycleScope.launch {
            try {
                doctorsRepository.getDoctorById(doctorId)
                    .onSuccess { doctorApi ->
                        if (_binding == null) return@onSuccess

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

                        val specialtyText =
                            if (!doctorApi.specializations.isNullOrEmpty()) {
                                doctorApi.specializations.joinToString(", ") { it.name }
                            } else {
                                doctorApi.bio ?: "Врач"
                            }

                        currentDoctor =
                            Doctor(
                                id = doctorApi.id.toString(),
                                name = fullName.ifEmpty { doctorApi.user.email },
                                specialty = specialtyText,
                                rating = doctorApi.rating ?: 0.0,
                                reviews = doctorApi.reviewCount ?: 0,
                                experience = "${doctorApi.experienceYears} лет",
                                location = "",
                                availability = "",
                                image = doctorApi.photoUrl.toImageDataUri(),
                                consultationFee = "", // Можно добавить поле в API если нужно
                            )

                        binding.doctorNameTextView.text = currentDoctor?.name
                        binding.doctorSpecialtyTextView.text = currentDoctor?.specialty

                        // Загружаем изображение
                        if (currentDoctor?.image?.isNotEmpty() == true) {
                            Glide.with(requireContext())
                                .load(currentDoctor?.image)
                                .placeholder(R.drawable.placeholder_doctor)
                                .error(R.drawable.placeholder_doctor)
                                .into(binding.doctorImageView)
                        } else {
                            binding.doctorImageView.setImageResource(R.drawable.placeholder_doctor)
                        }

                        Log.d("BookingFragment", "Доктор загружен: ${currentDoctor?.name}")
                    }
                    .onFailure { error ->
                        if (_binding == null) return@launch
                        Log.e("BookingFragment", "Ошибка загрузки доктора", error)
                        Toast.makeText(
                            requireContext(),
                            "Ошибка загрузки данных врача",
                            Toast.LENGTH_SHORT,
                        ).show()
                        findNavController().navigateUp()
                    }
            } catch (e: Exception) {
                Log.e("BookingFragment", "Исключение при загрузке доктора", e)
                Toast.makeText(requireContext(), "Ошибка загрузки данных врача", Toast.LENGTH_SHORT)
                    .show()
                findNavController().navigateUp()
            }
        }
    }

    private fun setupDateRecyclerView() {
        val dates = generateDatesForTwoWeeks()
        dateAdapter =
            DateAdapter(dates) { date ->
                selectedDate = date
                selectedTime = null // Сбрасываем выбранное время при смене даты
                loadAvailableTimes(date.dateString)
                updateConfirmButtonState()
            }
        binding.dateRecyclerView.adapter = dateAdapter
        binding.dateRecyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
    }

    private fun setupTimeRecyclerView() {
        val times = emptyList<TimeItem>()
        timeAdapter =
            TimeAdapter(times) { time ->
                if (!time.isBooked) {
                    selectedTime = time
                    updateConfirmButtonState()
                }
            }
        binding.timeRecyclerView.adapter = timeAdapter
        binding.timeRecyclerView.layoutManager = GridLayoutManager(context, 3)
    }

    private fun generateDatesForTwoWeeks(): List<DateItem> {
        val dates = mutableListOf<DateItem>()
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val dayFormatter = DateTimeFormatter.ofPattern("d")
        val monthFormatter = DateTimeFormatter.ofPattern("MMM", Locale("ru", "RU"))
        val locale = Locale("ru", "RU")

        for (i in 0 until 14) {
            val date = today.plusDays(i.toLong())
            val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
            val dayOfMonth = dayFormatter.format(date)
            val month = monthFormatter.format(date).replaceFirstChar { it.uppercaseChar() }
            val dateString = formatter.format(date)

            dates.add(DateItem(dayOfWeek, dayOfMonth, month, dateString, isSelected = false))
        }

        return dates
    }

    private fun loadAvailableTimes(date: String) {
        val doctorIdValue = doctorId ?: return

        lifecycleScope.launch {
            try {
                binding.timeRecyclerView.visibility = View.GONE
                binding.noAppointmentsCard.visibility = View.GONE
                // Можно добавить ProgressBar здесь

                appointmentRepository.getAvailableAppointments(
                    doctorIdValue,
                    date,
                    selectedServiceId,
                )
                    .onSuccess { appointments ->
                        if (_binding == null) return@launch

                        val today = LocalDate.now()
                        val selectedLocalDate = LocalDate.parse(date)
                        val currentTime = LocalTime.now()

                        val timeItems =
                            appointments.map { appointment ->
                                // Парсим время из startTime (формат: "2024-01-15T08:00:00Z")
                                val timeStr = parseTimeFromISO(appointment.startTime)
                                // Считаем слот занятым, если isBooked == true или patientId != null
                                val isBooked =
                                    appointment.isBooked == true || appointment.patientId != null

                                // Проверяем, прошёл ли слот (для сегодняшней даты)
                                val isPast =
                                    if (selectedLocalDate == today) {
                                        try {
                                            val slotTime = LocalTime.parse(timeStr)
                                            slotTime.isBefore(currentTime) || slotTime == currentTime
                                        } catch (e: Exception) {
                                            false
                                        }
                                    } else {
                                        false
                                    }

                                TimeItem(
                                    time = timeStr,
                                    appointmentId = appointment.id,
                                    isBooked = isBooked || isPast, // Прошедшие слоты считаем занятыми
                                    isSelected = false,
                                )
                            }.filter { !it.isBooked } // Показываем только свободные слоты (не занятые и не прошедшие)

                        if (timeItems.isEmpty()) {
                            // Показываем заглушку, если нет свободных слотов
                            binding.timeRecyclerView.visibility = View.GONE
                            binding.noAppointmentsCard.visibility = View.VISIBLE
                            selectedTime = null
                            updateConfirmButtonState()
                        } else {
                            // Показываем список доступного времени
                            binding.timeRecyclerView.visibility = View.VISIBLE
                            binding.noAppointmentsCard.visibility = View.GONE

                            timeAdapter =
                                TimeAdapter(timeItems) { time ->
                                    if (!time.isBooked) {
                                        selectedTime = time
                                        updateConfirmButtonState()
                                    }
                                }
                            binding.timeRecyclerView.adapter = timeAdapter
                        }
                    }
                    .onFailure { error ->
                        if (_binding == null) return@launch
                        Log.e("BookingFragment", "Ошибка загрузки доступного времени", error)
                        Toast.makeText(
                            requireContext(),
                            "Ошибка загрузки доступного времени",
                            Toast.LENGTH_SHORT,
                        ).show()
                        binding.timeRecyclerView.visibility = View.GONE
                        binding.noAppointmentsCard.visibility = View.VISIBLE
                    }
            } catch (e: Exception) {
                Log.e("BookingFragment", "Исключение при загрузке доступного времени", e)
                Toast.makeText(
                    requireContext(),
                    "Ошибка загрузки доступного времени",
                    Toast.LENGTH_SHORT,
                ).show()
                binding.timeRecyclerView.visibility = View.GONE
                binding.noAppointmentsCard.visibility = View.VISIBLE
            }
        }
    }

    private fun parseTimeFromISO(isoString: String): String {
        return try {
            // Формат: "2024-01-15T08:00:00Z" или "2024-01-15T08:00:00+00:00"
            val timePart = isoString.split("T")[1]
            val hourMinute = timePart.split(":")[0] + ":" + timePart.split(":")[1]
            hourMinute
        } catch (e: Exception) {
            Log.e("BookingFragment", "Ошибка парсинга времени: $isoString", e)
            "00:00"
        }
    }

    private fun updateConfirmButtonState() {
        val isEnabled = selectedDate != null && selectedTime != null && !selectedTime!!.isBooked && isSelectedTimeValid()
        binding.confirmBookingButton.isEnabled = isEnabled
    }

    /**
     * Проверяет, что выбранные дата и время не в прошлом
     */
    private fun isSelectedTimeValid(): Boolean {
        val date = selectedDate ?: return false
        val time = selectedTime ?: return false

        return try {
            val selectedLocalDate = LocalDate.parse(date.dateString)
            val selectedLocalTime = LocalTime.parse(time.time)
            val selectedDateTime = LocalDateTime.of(selectedLocalDate, selectedLocalTime)
            val now = LocalDateTime.now()

            selectedDateTime.isAfter(now)
        } catch (e: Exception) {
            Log.e("BookingFragment", "Ошибка проверки времени", e)
            false
        }
    }

    private fun confirmBooking() {
        val appointmentId = selectedTime?.appointmentId
        val userId = UserDataRepository.getCurrentUser()?.id

        if (appointmentId == null) {
            Toast.makeText(requireContext(), "Ошибка: не выбрано время", Toast.LENGTH_SHORT).show()
            return
        }

        if (userId == null) {
            Toast.makeText(requireContext(), "Ошибка: пользователь не найден", Toast.LENGTH_SHORT)
                .show()
            return
        }

        // Проверка, что выбранное время не в прошлом
        if (!isSelectedTimeValid()) {
            Toast.makeText(
                requireContext(),
                "Невозможно записаться на прошедшее время. Выберите другое время.",
                Toast.LENGTH_LONG,
            ).show()
            // Обновляем список доступных слотов
            selectedDate?.dateString?.let { loadAvailableTimes(it) }
            selectedTime = null
            updateConfirmButtonState()
            return
        }

        lifecycleScope.launch {
            try {
                binding.confirmBookingButton.isEnabled = false

                appointmentRepository.bookAppointment(appointmentId, userId, selectedServiceId)
                    .onSuccess { _ ->
                        if (_binding == null) return@launch

                        Log.d("BookingFragment", "Запись успешно создана")
                        val toastMsg = buildToastAfterBooking()
                        Toast.makeText(requireContext(), toastMsg, Toast.LENGTH_LONG).show()
                        showConfirmationScreen()
                    }
                    .onFailure { error ->
                        if (_binding == null) return@launch
                        Log.e("BookingFragment", "Ошибка создания записи", error)
                        Toast.makeText(
                            requireContext(),
                            "Ошибка создания записи: ${error.message}",
                            Toast.LENGTH_LONG,
                        ).show()
                        binding.confirmBookingButton.isEnabled = true
                    }
            } catch (e: Exception) {
                if (_binding == null) return@launch
                Log.e("BookingFragment", "Исключение при создании записи", e)
                Toast.makeText(requireContext(), "Ошибка создания записи", Toast.LENGTH_SHORT)
                    .show()
                binding.confirmBookingButton.isEnabled = true
            }
        }
    }

    private fun buildToastAfterBooking(): String {
        val sid = selectedServiceId ?: return getString(R.string.booking_success_toast_default)
        val svc = doctorServicesList.firstOrNull { it.id == sid } ?: return getString(R.string.booking_success_toast_default)
        val name = svc.name?.trim().orEmpty()
        if (name.isEmpty()) return getString(R.string.booking_success_toast_default)
        val rub = ServicePriceFormatter.formatRub(svc.price)
        return if (rub != null) {
            getString(R.string.booking_success_toast_with_price, name, rub)
        } else {
            getString(R.string.booking_success_toast_service_only, name)
        }
    }

    private fun showConfirmationScreen() {
        binding.bookingSelectionContainer.visibility = View.GONE
        binding.bookingConfirmationContainer.visibility = View.VISIBLE
        updateSummaryCard()
    }

    private fun showSelectionScreen() {
        binding.bookingConfirmationContainer.visibility = View.GONE
        binding.bookingSelectionContainer.visibility = View.VISIBLE
    }

    private fun updateSummaryCard() {
        val date =
            selectedDate?.let {
                val dateObj = LocalDate.parse(it.dateString)
                val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("ru", "RU"))
                formatter.format(dateObj)
            } ?: ""
        val time = selectedTime?.time ?: ""

        binding.summaryDate.text = "Дата: $date"
        binding.summaryTime.text = "Время: $time"

        val sid = selectedServiceId
        val svc = sid?.let { id -> doctorServicesList.firstOrNull { it.id == id } }
        if (svc != null && !svc.name.isNullOrBlank()) {
            binding.summaryServiceBlock.visibility = View.VISIBLE
            binding.summaryServiceLine.text =
                getString(R.string.booking_summary_service, svc.name!!.trim())
            val rub = ServicePriceFormatter.formatRub(svc.price)
            binding.summaryPriceLine.text =
                if (rub != null) {
                    getString(R.string.booking_summary_price, rub)
                } else {
                    getString(R.string.service_price_unknown)
                }
        } else {
            binding.summaryServiceBlock.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
