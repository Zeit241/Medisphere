package com.example.kursovaya.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.kursovaya.R
import com.example.kursovaya.activity.LoginActivity
import com.example.kursovaya.adapter.MenuAdapter
import com.example.kursovaya.databinding.FragmentProfileBinding
import com.example.kursovaya.model.MenuItem
import com.example.kursovaya.repository.AuthRepository
import com.example.kursovaya.repository.DoctorsRepository
import com.example.kursovaya.repository.UserRepository
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var authRepository: AuthRepository
    private lateinit var doctorsRepository: DoctorsRepository
    private lateinit var userRepository: UserRepository
    private lateinit var menuAdapter: MenuAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        // Убеждаемся, что RetrofitClient инициализирован
        com.example.kursovaya.api.RetrofitClient.init(requireContext())
        authRepository = AuthRepository(requireContext())
        doctorsRepository = DoctorsRepository(requireContext())
        userRepository = UserRepository(requireContext())
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupMenu()
        loadUserData()
    }

    private fun setupMenu() {
        menuAdapter = MenuAdapter()
        binding.menuRecyclerView.adapter = menuAdapter

        val menuItems =
            listOf(
                MenuItem(R.drawable.ic_person_dark, "Редактировать профиль") {
                    findNavController().navigate(R.id.editProfileFragment)
                },
                MenuItem(R.drawable.ic_calendar, "История посещений") {
                    findNavController().navigate(R.id.appointmentHistoryFragment)
                },
                MenuItem(R.drawable.ic_arrow_back, "Выйти") {
                    // Логика выхода из аккаунта
                    authRepository.clearAuth()
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                },
            )
        menuAdapter.submitList(menuItems)
    }

    private fun loadUserData() {
        // Получаем сохраненные данные пользователя
        val user = com.example.kursovaya.repository.UserDataRepository.getCurrentUser()

        if (user != null) {
            displayUserData(user)
        } else {
            // Если данных нет, показываем плейсхолдер
            binding.userNameTextView.text = "Пользователь"
            binding.userEmailTextView.text = ""
            binding.userImageView.setImageResource(R.drawable.ic_person)
        }

        // Загружаем статистику пользователя
        loadUserStats()
    }

    private fun loadUserStats() {
        lifecycleScope.launch {
            userRepository.getUserStats()
                .onSuccess { stats ->
                    Log.d(
                        "ProfileFragment",
                        "Статистика загружена: appointments=${stats.appointmentsCount}, reviews=${stats.reviewsCount}, queues=${stats.queueEntriesCount}",
                    )
                    binding.appointmentsCountTextView.text = stats.appointmentsCount.toString()
                    binding.reviewsCountTextView.text = stats.reviewsCount.toString()
                    binding.queueCountTextView.text = stats.queueEntriesCount.toString()
                }
                .onFailure { error ->
                    Log.e("ProfileFragment", "Ошибка загрузки статистики", error)
                    // Устанавливаем значения по умолчанию при ошибке
                    binding.appointmentsCountTextView.text = "0"
                    binding.reviewsCountTextView.text = "0"
                    binding.queueCountTextView.text = "0"
                }
        }
    }

    private fun displayUserData(user: com.example.kursovaya.model.api.User) {
        // Формируем полное имя: фамилия + имя + отчество
        val fullName =
            buildString {
                if (user.lastName.isNotEmpty()) {
                    append(user.lastName)
                }
                if (user.firstName.isNotEmpty()) {
                    if (isNotEmpty()) append(" ")
                    append(user.firstName)
                }
                if (!user.middleName.isNullOrEmpty()) {
                    if (isNotEmpty()) append(" ")
                    append(user.middleName)
                }
            }.ifEmpty { user.email }

        binding.userNameTextView.text = fullName
        binding.userEmailTextView.text = user.email
        binding.userImageView.setImageResource(R.drawable.ic_person)

        Log.d("ProfileFragment", "Отображены данные пользователя: $fullName, ${user.email}")
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
