package com.example.kursovaya.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.kursovaya.R
import com.example.kursovaya.adapter.ProfileViewPagerAdapter
import com.example.kursovaya.api.RetrofitClient
import com.example.kursovaya.databinding.FragmentDoctorProfileBinding
import com.example.kursovaya.model.DoctorProfile
import com.example.kursovaya.model.api.DoctorApi
import com.example.kursovaya.model.api.toImageDataUri
import com.example.kursovaya.repository.DoctorsRepository
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

class DoctorProfileFragment : Fragment() {
    private var _binding: FragmentDoctorProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var doctorsRepository: DoctorsRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentDoctorProfileBinding.inflate(inflater, container, false)
        // Убеждаемся, что RetrofitClient инициализирован
        com.example.kursovaya.api.RetrofitClient.init(requireContext())
        doctorsRepository = DoctorsRepository(requireContext())
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()

        val doctorId = arguments?.getString("DOCTOR_ID")
        val adapter = ProfileViewPagerAdapter(this, doctorId)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text =
                when (position) {
                    0 -> "О враче"
                    1 -> "Отзывы"
                    else -> null
                }
        }.attach()

        loadDoctorData(doctorId)

        binding.bookAppointmentButton.setOnClickListener {
            val bundle = bundleOf("DOCTOR_ID" to doctorId)
            findNavController().navigate(R.id.action_doctorProfileFragment_to_bookingFragment, bundle)
        }
    }

    private fun DoctorApi.toDoctorProfile(): DoctorProfile {
        val fullName =
            buildString {
                append(user.lastName)
                if (user.firstName.isNotEmpty()) {
                    append(" ${user.firstName}")
                }
                if (user.middleName?.isNotEmpty() == true) {
                    append(" ${user.middleName}")
                }
            }.trim()

        val specialtyText =
            if (!specializations.isNullOrEmpty()) {
                specializations.joinToString(", ") { it.name }
            } else {
                bio ?: "Врач"
            }

        return DoctorProfile(
            id = id.toString(),
            name = fullName.ifEmpty { user.email },
            specialty = specialtyText,
            rating = rating ?: 0.0,
            reviewCount = reviewCount ?: 0,
            experience = "$experienceYears лет",
            location = "",
            availability = "",
            image = photoUrl.toImageDataUri(),
            consultationFee = "",
            about = bio ?: "",
            education = emptyList(),
            reviews = emptyList(),
        )
    }

    private fun setupToolbar() {
        (activity as? AppCompatActivity)?.setSupportActionBar(binding.toolbar)
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
    }

    private fun loadDoctorData(doctorId: String?) {
        if (doctorId == null) {
            Snackbar.make(binding.root, "ID врача не указан", Snackbar.LENGTH_SHORT).show()
            return
        }

        val id = doctorId.toLongOrNull()
        if (id == null) {
            Snackbar.make(binding.root, "Неверный ID врача", Snackbar.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                doctorsRepository.getDoctorById(id)
                    .onSuccess { doctorApi ->
                        if (_binding == null) return@onSuccess

                        val doctor = doctorApi.toDoctorProfile()

                        binding.doctorNameTextView.text = doctor.name
                        binding.doctorSpecialtyTextView.text = doctor.specialty
                        binding.doctorRatingTextView.text = doctor.rating.toString()
                        binding.doctorReviewsTextView.text = "(${doctor.reviewCount} отзывов)"

                        // Загружаем изображение
                        if (doctor.image.isNotEmpty()) {
                            Glide.with(requireContext())
                                .load(doctor.image)
                                .placeholder(R.drawable.placeholder_doctor)
                                .error(R.drawable.placeholder_doctor)
                                .into(binding.doctorImageView)
                        } else {
                            val imageResId =
                                resources.getIdentifier(
                                    "placeholder_doctor",
                                    "drawable",
                                    requireContext().packageName,
                                )
                            if (imageResId != 0) {
                                binding.doctorImageView.setImageResource(imageResId)
                            }
                        }

                        Log.d("DoctorProfileFragment", "Loaded doctor: ${doctor.name}")
                    }
                    .onFailure { error ->
                        if (_binding == null) return@launch
                        Log.e("DoctorProfileFragment", "Error loading doctor", error)
                        Snackbar.make(
                            binding.root,
                            "Ошибка загрузки данных врача: ${error.message}",
                            Snackbar.LENGTH_LONG,
                        ).show()
                    }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                if (_binding == null) return@launch
                Log.e("DoctorProfileFragment", "Unexpected error", e)
                Snackbar.make(
                    binding.root,
                    "Ошибка загрузки данных врача",
                    Snackbar.LENGTH_LONG,
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
