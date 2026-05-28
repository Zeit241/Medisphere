package com.example.kursovaya.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.kursovaya.R
import com.example.kursovaya.adapter.AppointmentAdapter
import com.example.kursovaya.model.Appointment
import com.example.kursovaya.model.AppointmentStatus
import com.example.kursovaya.repository.AppointmentRepository
import com.example.kursovaya.repository.UserDataRepository
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class AppointmentHistoryFragment : Fragment() {
    private lateinit var tabLayout: TabLayout
    private lateinit var appointmentsRecyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var bookAppointmentButton: Button

    private lateinit var appointmentAdapter: AppointmentAdapter
    private lateinit var appointmentRepository: AppointmentRepository
    private var allAppointments: List<Appointment> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_appointment_history, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        appointmentRepository = AppointmentRepository(requireContext())

        val toolbar: Toolbar = view.findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        tabLayout = view.findViewById(R.id.tabLayout)
        appointmentsRecyclerView = view.findViewById(R.id.appointmentsRecyclerView)
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout)
        bookAppointmentButton = view.findViewById(R.id.bookAppointmentButton)

        setupTabs()
        setupRecyclerView()
        setupEmptyStateButton()
        loadAppointments()
    }

    private fun setupEmptyStateButton() {
        bookAppointmentButton.setOnClickListener {
            findNavController().navigate(R.id.action_appointmentHistoryFragment_to_doctorsFragment)
        }
    }

    override fun onResume() {
        super.onResume()
        // Обновляем список при возврате на экран (например, после отмены в детальном виде)
        loadAppointments()
    }

    private fun setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Все"))
        tabLayout.addTab(tabLayout.newTab().setText("Предстоящие"))
        tabLayout.addTab(tabLayout.newTab().setText("Завершены"))
        tabLayout.addTab(tabLayout.newTab().setText("Отмененные"))

        tabLayout.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    filterAppointments(tab?.text.toString())
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {}

                override fun onTabReselected(tab: TabLayout.Tab?) {}
            },
        )
    }

    private fun setupRecyclerView() {
        appointmentAdapter =
            AppointmentAdapter(
                appointments = emptyList(),
                onAppointmentClick = { appointment ->
                    val action = AppointmentHistoryFragmentDirections.actionAppointmentHistoryFragmentToAppointmentDetailsFragment(appointment)
                    findNavController().navigate(action)
                },
                onNavigateClick = { appointment ->
                    navigateToRoom(appointment)
                },
                onCancelClick = { appointment ->
                    showCancelDrawer(appointment)
                },
                onRepeatClick = { appointment ->
                    navigateToBooking(appointment)
                },
            )
        appointmentsRecyclerView.adapter = appointmentAdapter
    }

    private fun navigateToBooking(appointment: Appointment) {
        val bundle =
            Bundle().apply {
                putString("DOCTOR_ID", appointment.doctorId)
            }
        findNavController().navigate(R.id.action_appointmentHistoryFragment_to_bookingFragment, bundle)
    }

    private fun navigateToRoom(appointment: Appointment) {
        Log.d("AppointmentHistory", "=== Навигация на карту ===")
        Log.d("AppointmentHistory", "ID записи: ${appointment.id}")
        Log.d("AppointmentHistory", "Врач: ${appointment.doctorName}")
        Log.d("AppointmentHistory", "Специальность: ${appointment.specialty}")
        Log.d("AppointmentHistory", "Код кабинета (roomCode): ${appointment.roomCode}")
        Log.d("AppointmentHistory", "Название кабинета (roomName): ${appointment.roomName}")
        Log.d("AppointmentHistory", "Дата: ${appointment.date}")
        Log.d("AppointmentHistory", "Время: ${appointment.time}")
        Log.d("AppointmentHistory", "==========================")

        val bundle =
            Bundle().apply {
                putString("roomId", appointment.roomCode)
            }
        findNavController().navigate(R.id.nav_map, bundle)
    }

    private fun loadAppointments() {
        val user = UserDataRepository.getCurrentUser()
        val patientId = user?.patientId

        if (patientId == null) {
            Toast.makeText(requireContext(), "Ошибка: не найден ID пациента", Toast.LENGTH_SHORT).show()
            showEmptyState()
            return
        }

        lifecycleScope.launch {
            try {
                val result = appointmentRepository.getAppointmentsForPatient(patientId)
                if (result.isSuccess) {
                    allAppointments = result.getOrNull() ?: emptyList()
                    // Сохраняем текущую выбранную вкладку
                    val currentTab = tabLayout.getTabAt(tabLayout.selectedTabPosition)?.text?.toString() ?: "Все"
                    filterAppointments(currentTab)
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Неизвестная ошибка"
                    Toast.makeText(requireContext(), "Ошибка загрузки записей: $error", Toast.LENGTH_SHORT).show()
                    showEmptyState()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                showEmptyState()
            }
        }
    }

    private fun filterAppointments(status: String) {
        val filteredList =
            when (status) {
                "Предстоящие" -> allAppointments.filter { it.status == AppointmentStatus.UPCOMING }
                "Завершены" -> allAppointments.filter { it.status == AppointmentStatus.COMPLETED }
                "Отмененные" -> allAppointments.filter { it.status == AppointmentStatus.CANCELLED }
                else -> allAppointments
            }

        // Показывать статус только на вкладке "Все"
        val showStatus = status == "Все"
        appointmentAdapter.updateAppointments(filteredList, showStatus)

        if (filteredList.isEmpty()) {
            appointmentsRecyclerView.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE
        } else {
            appointmentsRecyclerView.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE
        }
    }

    private fun showEmptyState() {
        appointmentsRecyclerView.visibility = View.GONE
        emptyStateLayout.visibility = View.VISIBLE
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
                        // Обновляем список после отмены
                        loadAppointments()
                    }
                    .onFailure { error ->
                        Log.e("AppointmentHistoryFragment", "Ошибка отмены записи", error)
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
