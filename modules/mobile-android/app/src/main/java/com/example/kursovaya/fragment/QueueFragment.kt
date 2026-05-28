package com.example.kursovaya.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kursovaya.R
import com.example.kursovaya.adapter.QueueAdapter
import com.example.kursovaya.databinding.FragmentQueueBinding
import com.example.kursovaya.model.QueueItem
import com.example.kursovaya.model.api.toImageDataUri
import com.example.kursovaya.model.websocket.QueueEntry
import com.example.kursovaya.model.websocket.QueueResponse
import com.example.kursovaya.model.websocket.QueueUpdate
import com.example.kursovaya.notification.QueueNotificationManager
import com.example.kursovaya.repository.AuthRepository
import com.example.kursovaya.repository.DoctorsRepository
import com.example.kursovaya.viewmodel.QueueSocketViewModel
import com.example.kursovaya.viewmodel.QueueSocketViewModelFactory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class QueueFragment : Fragment() {
    private var _binding: FragmentQueueBinding? = null
    private val binding get() = _binding!!
    private lateinit var queueAdapter: QueueAdapter
    private val socketViewModel: QueueSocketViewModel by activityViewModels {
        QueueSocketViewModelFactory(requireActivity().application)
    }
    private lateinit var authRepository: AuthRepository
    private lateinit var doctorsRepository: DoctorsRepository
    private val gson = Gson()

    // Кэш для данных о врачах
    private val doctorCache = mutableMapOf<Long, com.example.kursovaya.model.api.DoctorApi>()

    // Храним ID врачей, на обновления которых подписаны
    private val subscribedDoctorIds = mutableSetOf<Long>()

    // Храним предыдущие позиции для отслеживания изменений
    private val previousPositions = mutableMapOf<Long, Int>() // doctorId -> position

    // Храним patientId и appointmentId для каждого врача, чтобы найти позицию в обновленной очереди
    private val userQueueEntries = mutableMapOf<Long, Pair<Long?, Long?>>() // doctorId -> (patientId?, appointmentId?)

    // Менеджер уведомлений
    private lateinit var notificationManager: QueueNotificationManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentQueueBinding.inflate(inflater, container, false)

        // Инициализация репозиториев
        com.example.kursovaya.api.RetrofitClient.init(requireContext())
        authRepository = AuthRepository(requireContext())
        doctorsRepository = DoctorsRepository(requireContext())

        // Инициализация менеджера уведомлений
        notificationManager = QueueNotificationManager(requireContext())

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupToolbar()
        setupEmptyStateButton()
        // Показываем пустое состояние по умолчанию, пока данные не загрузятся
        showEmptyState()
        // Запрашиваем разрешение на уведомления
        requestNotificationPermission()

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    socketViewModel.queueResponses.collect { handleWebSocketMessage(it) }
                }
                launch {
                    socketViewModel.queueUpdates.collect { handleQueueUpdate(it) }
                }
                launch {
                    socketViewModel.socketErrors.collect { showError(it) }
                }
                launch {
                    socketViewModel.disconnectEvents.collect {
                        subscribedDoctorIds.clear()
                    }
                }
            }
        }

        if (socketViewModel.isConnected()) {
            socketViewModel.getMyQueues()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d("QueueFragment", "Notification permission already granted")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Показываем объяснение, почему нужно разрешение
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Для получения уведомлений об изменениях в очереди необходимо разрешение",
                        android.widget.Toast.LENGTH_LONG,
                    ).show()
                    requestPermissions(
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        REQUEST_NOTIFICATION_PERMISSION,
                    )
                }
                else -> {
                    // Запрашиваем разрешение
                    requestPermissions(
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        REQUEST_NOTIFICATION_PERMISSION,
                    )
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_NOTIFICATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("QueueFragment", "Notification permission granted")
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Уведомления включены",
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                } else {
                    Log.w("QueueFragment", "Notification permission denied")
                    showNotificationSettingsDialog()
                }
            }
        }
    }

    private fun showNotificationSettingsDialog() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Разрешение на уведомления")
            .setMessage("Для получения уведомлений об изменениях в очереди необходимо разрешение. Хотите открыть настройки?")
            .setPositiveButton("Открыть настройки") { _, _ ->
                try {
                    val intent =
                        android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
                        }
                    startActivity(intent)
                } catch (e: Exception) {
                    // Если не удалось открыть настройки уведомлений, открываем общие настройки приложения
                    try {
                        val intent =
                            android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = android.net.Uri.parse("package:${requireContext().packageName}")
                            }
                        startActivity(intent)
                    } catch (e2: Exception) {
                        android.widget.Toast.makeText(
                            requireContext(),
                            "Откройте настройки приложения вручную и включите уведомления",
                            android.widget.Toast.LENGTH_LONG,
                        ).show()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    companion object {
        private const val REQUEST_NOTIFICATION_PERMISSION = 1001
    }

    private fun setupRecyclerView() {
        queueAdapter = QueueAdapter()
        binding.queueRecyclerView.apply {
            adapter = queueAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupEmptyStateButton() {
        binding.bookAppointmentButton.setOnClickListener {
            // Переход на страницу с докторами при пустой очереди
            findNavController().navigate(R.id.nav_doctors)
        }
    }

    private fun handleWebSocketMessage(response: QueueResponse) {
        if (!response.success) {
            Log.e("QueueFragment", "Error from server: ${response.message}")
            showError(response.message)
            // Если ошибка, показываем пустое состояние
            showEmptyState()
            return
        }

        when {
            // Ответ на /app/queue/init или /app/queue/my-queues
            response.data is List<*> -> {
                val queueEntries =
                    try {
                        val listType = object : TypeToken<List<QueueEntry>>() {}.type
                        gson.fromJson<List<QueueEntry>>(gson.toJson(response.data), listType)
                    } catch (e: Exception) {
                        Log.e("QueueFragment", "Error parsing queue entries", e)
                        emptyList()
                    }

                if (queueEntries.isNotEmpty()) {
                    loadQueueItems(queueEntries)
                } else {
                    Log.d("QueueFragment", "No queue entries found, showing empty state")
                    showEmptyState()
                }
            }

            // Ответ на /app/queue/position
            response.data is Map<*, *> -> {
                val positionData =
                    try {
                        gson.fromJson(gson.toJson(response.data), com.example.kursovaya.model.websocket.PositionData::class.java)
                    } catch (e: Exception) {
                        Log.e("QueueFragment", "Error parsing position data", e)
                        null
                    }

                positionData?.let {
                    // Обновляем позицию для конкретного врача
                    updateQueuePosition(it)
                }
            }

            // Если data null или пустой
            response.data == null -> {
                Log.d("QueueFragment", "Response data is null, showing empty state")
                showEmptyState()
            }
        }
    }

    private fun loadQueueItems(queueEntries: List<QueueEntry>) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Показываем индикатор загрузки
                binding.queueRecyclerView.visibility = View.GONE
                binding.emptyStateLayout.visibility = View.GONE

                // Загружаем информацию о врачах параллельно
                val queueItems =
                    queueEntries.map { entry ->
                        async {
                            val doctor = getDoctorInfo(entry.doctorId)
                            mapQueueEntryToQueueItem(entry, doctor)
                        }
                    }.awaitAll()

                // Обновляем UI
                if (queueItems.isNotEmpty()) {
                    binding.queueRecyclerView.visibility = View.VISIBLE
                    binding.emptyStateLayout.visibility = View.GONE
                    queueAdapter.submitList(queueItems)

                    // Сохраняем текущие позиции и данные пользователя для отслеживания изменений
                    queueEntries.forEach { entry ->
                        previousPositions[entry.doctorId] = entry.position
                        userQueueEntries[entry.doctorId] = Pair(entry.patientId, entry.appointmentId)
                        Log.d(
                            "QueueFragment",
                            "Saved entry for doctor ${entry.doctorId}: patientId=${entry.patientId}, appointmentId=${entry.appointmentId}, position=${entry.position}",
                        )
                    }

                    // Подписываемся на обновления очередей для всех врачей
                    val doctorIds = queueEntries.map { it.doctorId }.distinct()
                    Log.d("QueueFragment", "Subscribing to ${doctorIds.size} doctors: $doctorIds")
                    subscribeToQueueUpdates(doctorIds)
                } else {
                    showEmptyState()
                }
            } catch (e: Exception) {
                Log.e("QueueFragment", "Error loading queue items", e)
                showError("Ошибка загрузки данных очереди")
            }
        }
    }

    private suspend fun getDoctorInfo(doctorId: Long): com.example.kursovaya.model.api.DoctorApi? {
        // Проверяем кэш
        doctorCache[doctorId]?.let { return it }

        // Загружаем из API
        return doctorsRepository.getDoctorById(doctorId).getOrElse { error ->
            Log.e("QueueFragment", "Error loading doctor $doctorId", error)
            null
        }?.also { doctor ->
            doctorCache[doctorId] = doctor
        }
    }

    private fun mapQueueEntryToQueueItem(
        entry: QueueEntry,
        doctor: com.example.kursovaya.model.api.DoctorApi?,
    ): QueueItem {
        val doctorName =
            if (doctor != null) {
                buildString {
                    append(doctor.user.lastName)
                    if (doctor.user.firstName.isNotEmpty()) {
                        append(" ${doctor.user.firstName}")
                    }
                    if (doctor.user.middleName?.isNotEmpty() == true) {
                        append(" ${doctor.user.middleName}")
                    }
                }.trim().ifEmpty { "Врач #${entry.doctorId}" }
            } else {
                "Врач #${entry.doctorId}"
            }

        val specialty =
            if (doctor != null && !doctor.specializations.isNullOrEmpty()) {
                doctor.specializations.joinToString(", ") { it.name }
            } else {
                "Врач"
            }

        val image =
            if (doctor?.photoUrl != null && doctor.photoUrl.isNotEmpty()) {
                doctor.photoUrl.toImageDataUri()
            } else {
                "placeholder_doctor"
            }

        // Определяем статус на основе позиции
        val status =
            when {
                entry.position == 0 -> "ready"
                entry.position == 1 -> "next"
                else -> "waiting"
            }

        // Оценка времени ожидания (примерно 15 минут на человека)
        val estimatedWaitMinutes = entry.position * 15
        val estimatedWait =
            when {
                estimatedWaitMinutes < 60 -> "$estimatedWaitMinutes мин"
                else -> "${estimatedWaitMinutes / 60} ч ${estimatedWaitMinutes % 60} мин"
            }

        return QueueItem(
            id = entry.id?.toString() ?: "${entry.doctorId}_${entry.appointmentId ?: entry.patientId}",
            doctorName = doctorName,
            specialty = specialty,
            currentNumber = 0, // Текущий номер будет обновляться через position endpoint
            yourNumber = entry.position + 1,
            estimatedWait = estimatedWait,
            status = status,
            peopleAhead = entry.position,
            image = image,
        )
    }

    private fun updateQueuePosition(positionData: com.example.kursovaya.model.websocket.PositionData) {
        // Обновляем позицию для конкретного элемента очереди
        val currentList = queueAdapter.currentList.toMutableList()
        val queueEntryIdStr = positionData.queueEntryId.toString()
        val index = currentList.indexOfFirst { item -> item.id == queueEntryIdStr }

        if (index >= 0) {
            val existingItem = currentList[index]
            val updatedItem =
                existingItem.copy(
                    currentNumber = positionData.position,
                    yourNumber = positionData.position + 1,
                    status =
                        if (positionData.isNext) {
                            "next"
                        } else if (positionData.position == 0) {
                            "ready"
                        } else {
                            "waiting"
                        },
                    peopleAhead = positionData.position,
                )
            currentList[index] = updatedItem
            queueAdapter.submitList(currentList)
        }
    }

    private fun showEmptyState() {
        if (_binding == null) return
        binding.queueRecyclerView.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.VISIBLE
        queueAdapter.submitList(emptyList())
        Log.d("QueueFragment", "Empty state shown")
    }

    private fun showError(message: String) {
        // Можно показать Snackbar или Toast
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
    }

    /**
     * Подписывается на обновления очередей для указанных врачей
     */
    private fun subscribeToQueueUpdates(doctorIds: List<Long>) {
        Log.d("QueueFragment", "subscribeToQueueUpdates called with ${doctorIds.size} doctors")
        doctorIds.forEach { doctorId ->
            if (!subscribedDoctorIds.contains(doctorId)) {
                Log.d("QueueFragment", "Subscribing to queue updates for doctor $doctorId")
                socketViewModel.subscribeToQueueUpdates(doctorId)
                subscribedDoctorIds.add(doctorId)
                Log.d("QueueFragment", "Successfully subscribed to doctor $doctorId. Total subscribed: ${subscribedDoctorIds.size}")
            } else {
                Log.d("QueueFragment", "Already subscribed to doctor $doctorId")
            }
        }
        Log.d("QueueFragment", "Final subscribed doctors: $subscribedDoctorIds")
    }

    /**
     * Обрабатывает обновление очереди от сервера
     */
    private fun handleQueueUpdate(update: QueueUpdate) {
        Log.d("QueueFragment", "Handling queue update for doctor ${update.doctorId}, queue size: ${update.queue.size}")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Получаем информацию о враче
                val doctor = getDoctorInfo(update.doctorId)
                val doctorName =
                    doctor?.let {
                        buildString {
                            append(it.user.lastName)
                            if (it.user.firstName.isNotEmpty()) {
                                append(" ${it.user.firstName}")
                            }
                        }.trim()
                    } ?: "Врач #${update.doctorId}"

                // Находим позицию пользователя в обновленной очереди
                val userEntry = userQueueEntries[update.doctorId]
                if (userEntry != null && update.queue.isNotEmpty()) {
                    val (patientId, appointmentId) = userEntry

                    // Ищем запись пользователя в обновленной очереди
                    val userQueueEntry =
                        update.queue.find { entry ->
                            (patientId != null && entry.patientId == patientId) ||
                                (appointmentId != null && entry.appointmentId == appointmentId)
                        }

                    if (userQueueEntry != null) {
                        val newPosition = userQueueEntry.position
                        val previousPosition = previousPositions[update.doctorId]

                        Log.d(
                            "QueueFragment",
                            "Found user in queue for doctor ${update.doctorId}: position=$newPosition, previous=$previousPosition",
                        )

                        // Отправляем уведомление при изменении позиции
                        if (previousPosition != null && previousPosition != newPosition) {
                            Log.d("QueueFragment", "Sending notification: position changed from $previousPosition to $newPosition")
                            notificationManager.showQueuePositionUpdate(
                                doctorName = doctorName,
                                newPosition = newPosition,
                                previousPosition = previousPosition,
                            )
                        } else if (previousPosition == null) {
                            // Первое уведомление при подписке
                            Log.d("QueueFragment", "Sending first notification: position=$newPosition")
                            notificationManager.showQueueApproaching(doctorName, newPosition)
                        } else if (newPosition <= 2 && previousPosition > 2) {
                            // Уведомление, когда очередь приближается
                            Log.d("QueueFragment", "Sending approaching notification: position=$newPosition")
                            notificationManager.showQueueApproaching(doctorName, newPosition)
                        }

                        // Обновляем сохраненную позицию
                        previousPositions[update.doctorId] = newPosition
                    } else {
                        Log.d("QueueFragment", "User not found in updated queue for doctor ${update.doctorId}")
                        // Пользователя нет в очереди - возможно его удалили
                        previousPositions.remove(update.doctorId)
                        userQueueEntries.remove(update.doctorId)
                    }
                } else {
                    Log.d("QueueFragment", "No user entry found for doctor ${update.doctorId} or queue is empty")
                }

                // Перезагружаем все очереди для получения актуальных данных
                if (socketViewModel.isConnected()) {
                    Log.d("QueueFragment", "Reloading queues after update for doctor ${update.doctorId}")
                    socketViewModel.getMyQueues()
                }
            } catch (e: Exception) {
                Log.e("QueueFragment", "Error handling queue update", e)
            }
        }
    }

    override fun onDestroyView() {
        subscribedDoctorIds.clear()
        previousPositions.clear()
        userQueueEntries.clear()
        _binding = null
        super.onDestroyView()
    }
}
