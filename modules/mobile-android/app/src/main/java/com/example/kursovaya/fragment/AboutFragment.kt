package com.example.kursovaya.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.kursovaya.api.RetrofitClient
import com.example.kursovaya.databinding.FragmentAboutBinding
import com.example.kursovaya.repository.DoctorsRepository
import kotlinx.coroutines.launch

class AboutFragment : Fragment() {
    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!
    private lateinit var doctorsRepository: DoctorsRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        // Убеждаемся, что RetrofitClient инициализирован
        RetrofitClient.init(requireContext())
        doctorsRepository = DoctorsRepository(requireContext())
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        loadDoctorData()
    }

    private fun loadDoctorData() {
        val doctorId = arguments?.getString("DOCTOR_ID")
        if (doctorId == null) {
            binding.aboutTextView.text = "ID врача не указан"
            return
        }

        val id = doctorId.toLongOrNull()
        if (id == null) {
            binding.aboutTextView.text = "Неверный ID врача"
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                doctorsRepository.getDoctorById(id)
                    .onSuccess { doctorApi ->
                        if (_binding == null) return@onSuccess

                        val aboutText = doctorApi.bio ?: "Информация о враче отсутствует"
                        binding.aboutTextView.text = aboutText

                        Log.d("AboutFragment", "Loaded doctor bio")
                    }
                    .onFailure { error ->
                        if (_binding == null) return@launch
                        Log.e("AboutFragment", "Error loading doctor", error)
                        binding.aboutTextView.text = "Ошибка загрузки информации о враче"
                    }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                if (_binding == null) return@launch
                Log.e("AboutFragment", "Unexpected error", e)
                binding.aboutTextView.text = "Ошибка загрузки информации о враче"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
