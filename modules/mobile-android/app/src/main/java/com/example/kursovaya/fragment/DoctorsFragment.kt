package com.example.kursovaya.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kursovaya.R
import com.example.kursovaya.adapter.DoctorsAdapter
import com.example.kursovaya.databinding.FragmentDoctorsBinding
import com.example.kursovaya.model.Doctor
import com.example.kursovaya.model.api.DoctorApi
import com.example.kursovaya.model.api.Specialization
import com.example.kursovaya.model.api.toImageDataUri
import com.example.kursovaya.repository.DoctorsRepository
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DoctorsFragment : Fragment() {
    companion object {
        private const val PAGE_SIZE = 20
        private const val LOAD_MORE_THRESHOLD = 4
    }

    private var _binding: FragmentDoctorsBinding? = null
    private val binding get() = _binding!!
    private lateinit var doctorsRepository: DoctorsRepository
    private lateinit var doctorsAdapter: DoctorsAdapter

    /** Строка запроса для API-параметра `q` (поиск и фильтр по специальности на бэкенде). */
    private var listQuery: String? = null
    private var currentOffset = 0
    private var hasMore = true
    private var listGeneration = 0
    private var isPagingLoading = false
    private var refreshJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentDoctorsBinding.inflate(inflater, container, false)
        com.example.kursovaya.api.RetrofitClient.init(requireContext())
        doctorsRepository = DoctorsRepository(requireContext())
        return binding.root
    }

    private var initialSpecialization: String? = null

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        initialSpecialization = arguments?.getString("specialization")?.trim()?.takeIf { it.isNotEmpty() }

        setupRecyclerView()
        setupSearch()
        loadSpecializations()

        if (initialSpecialization == null) {
            listQuery = null
            loadDoctors(reset = true)
        }
    }

    private fun setupRecyclerView() {
        doctorsAdapter =
            DoctorsAdapter(
                doctors = emptyList(),
                onDoctorClicked = { doctor ->
                    val bundle = bundleOf("DOCTOR_ID" to doctor.id)
                    findNavController().navigate(
                        R.id.action_doctorsFragment_to_doctorProfileFragment,
                        bundle,
                    )
                },
                onBookClicked = { doctor ->
                    val bundle = bundleOf("DOCTOR_ID" to doctor.id)
                    findNavController().navigate(
                        R.id.action_doctorsFragment_to_bookingFragment,
                        bundle,
                    )
                },
            )

        binding.recyclerViewDoctors.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = doctorsAdapter
            addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(
                        recyclerView: RecyclerView,
                        dx: Int,
                        dy: Int,
                    ) {
                        if (dy <= 0) return
                        val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
                        val total = lm.itemCount
                        if (total == 0) return
                        val lastVisible = lm.findLastVisibleItemPosition()
                        if (lastVisible == RecyclerView.NO_POSITION) return
                        if (lastVisible >= total - LOAD_MORE_THRESHOLD) {
                            loadNextPage()
                        }
                    }
                },
            )
        }
    }

    private var searchJob: Job? = null
    private var isProgrammaticTextChange = false

    private fun setupSearch() {
        binding.searchButton.setOnClickListener {
            performSearch()
        }

        binding.searchInputEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }

        binding.searchInputEditText.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int,
                ) {}

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int,
                ) {}

                override fun afterTextChanged(s: Editable?) {
                    if (isProgrammaticTextChange) {
                        return
                    }

                    searchJob?.cancel()
                    searchJob =
                        viewLifecycleOwner.lifecycleScope.launch {
                            delay(500)
                            if (_binding != null) {
                                performSearch()
                            }
                        }
                }
            },
        )
    }

    private fun loadSpecializations() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                doctorsRepository.getSpecializations()
                    .onSuccess { specializations ->
                        if (_binding == null) return@onSuccess
                        setupFilters(specializations)
                        Log.d("DoctorsFragment", "Loaded ${specializations.size} specializations")
                    }
                    .onFailure { error ->
                        if (_binding == null) return@launch
                        Log.e("DoctorsFragment", "Error loading specializations", error)
                        if (initialSpecialization != null) {
                            listQuery = initialSpecialization
                            loadDoctors(reset = true)
                            initialSpecialization = null
                        }
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (_binding == null) return@launch
                Log.e("DoctorsFragment", "Unexpected error loading specializations", e)
            }
        }
    }

    private fun setupFilters(specializations: List<Specialization>) {
        binding.specialtyChipGroup.removeAllViews()

        var chipToSelect: Chip? = null

        for (specialization in specializations) {
            val chip =
                Chip(requireContext()).apply {
                    text = specialization.name
                    isClickable = true
                    isCheckable = true
                    id = View.generateViewId()
                    tag = specialization.name
                }

            binding.specialtyChipGroup.addView(chip)

            if (initialSpecialization != null &&
                specialization.name.equals(initialSpecialization, ignoreCase = true)
            ) {
                chipToSelect = chip
            }
        }

        binding.specialtyChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            searchJob?.cancel()

            if (checkedIds.isEmpty()) {
                isProgrammaticTextChange = true
                binding.searchInputEditText.setText("")
                binding.searchInputEditText.post {
                    isProgrammaticTextChange = false
                }
                listQuery = null
                loadDoctors(reset = true)
            } else {
                val checkedChipId = checkedIds[0]
                val checkedChip = group.findViewById<Chip>(checkedChipId)
                checkedChip?.let {
                    val specialtyName = it.text.toString()
                    isProgrammaticTextChange = true
                    binding.searchInputEditText.setText("")
                    binding.searchInputEditText.post {
                        isProgrammaticTextChange = false
                    }
                    searchBySpecialty(specialtyName)
                }
            }
        }

        when {
            chipToSelect != null -> {
                val selectChip = chipToSelect
                selectChip.post {
                    selectChip.isChecked = true
                }
                initialSpecialization = null
            }
            initialSpecialization != null -> {
                listQuery = initialSpecialization
                loadDoctors(reset = true)
                initialSpecialization = null
            }
        }
    }

    private fun searchBySpecialty(specialty: String) {
        listQuery = specialty
        loadDoctors(reset = true)
    }

    private fun performSearch() {
        val query = binding.searchInputEditText.text.toString().trim()
        if (query.isNotEmpty()) {
            clearChipSelection()
        }
        listQuery = query.takeIf { it.isNotEmpty() }
        loadDoctors(reset = true)
    }

    private fun clearChipSelection() {
        val checkedChipId = binding.specialtyChipGroup.checkedChipId
        if (checkedChipId != View.NO_ID) {
            binding.specialtyChipGroup.clearCheck()
        }
    }

    /**
     * Первая страница или смена фильтра/поиска.
     */
    private fun loadDoctors(reset: Boolean) {
        if (!reset) {
            loadNextPage()
            return
        }

        refreshJob?.cancel()
        listGeneration++
        val generation = listGeneration
        currentOffset = 0
        hasMore = true
        doctorsAdapter.updateDoctors(emptyList())

        refreshJob =
            viewLifecycleOwner.lifecycleScope.launch {
                if (_binding == null) return@launch
                binding.initialLoadingProgress.visibility = View.VISIBLE
                binding.recyclerViewDoctors.visibility = View.GONE
                binding.emptyStateLayout.visibility = View.GONE

                try {
                    val result =
                        doctorsRepository.getDoctors(
                            query = listQuery?.takeIf { it.isNotBlank() },
                            limit = PAGE_SIZE,
                            offset = 0,
                            sortBy = null,
                            sortOrder = null,
                        )

                    if (generation != listGeneration || _binding == null) return@launch

                    result.onSuccess { apiList ->
                        if (generation != listGeneration || _binding == null) return@onSuccess

                        val mapped = apiList.map { it.toDoctor() }
                        doctorsAdapter.updateDoctors(mapped)
                        currentOffset = apiList.size
                        hasMore = apiList.size >= PAGE_SIZE

                        val count = doctorsAdapter.itemCount
                        updateResultsCount(count)
                        updateEmptyState(count == 0)
                        if (count > 0) {
                            binding.recyclerViewDoctors.visibility = View.VISIBLE
                        }
                        Log.d("DoctorsFragment", "Loaded first page: ${apiList.size} doctors, hasMore=$hasMore")
                    }.onFailure { error ->
                        if (error is CancellationException) throw error
                        if (_binding == null) return@onFailure
                        Log.e("DoctorsFragment", "Error loading doctors", error)
                        Snackbar.make(
                            binding.root,
                            "Ошибка загрузки врачей: ${error.message}",
                            Snackbar.LENGTH_LONG,
                        ).show()
                        updateEmptyState(true)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (_binding == null) return@launch
                    Log.e("DoctorsFragment", "Unexpected error", e)
                } finally {
                    if (_binding != null) {
                        binding.initialLoadingProgress.visibility = View.GONE
                    }
                }
            }
    }

    private fun loadNextPage() {
        if (!hasMore || isPagingLoading) return
        val generation = listGeneration

        viewLifecycleOwner.lifecycleScope.launch {
            if (!hasMore || isPagingLoading || generation != listGeneration || _binding == null) {
                return@launch
            }
            isPagingLoading = true
            binding.loadMoreFooter.visibility = View.VISIBLE

            try {
                val result =
                    doctorsRepository.getDoctors(
                        query = listQuery?.takeIf { it.isNotBlank() },
                        limit = PAGE_SIZE,
                        offset = currentOffset,
                        sortBy = null,
                        sortOrder = null,
                    )

                if (generation != listGeneration || _binding == null) return@launch

                result.onSuccess { apiList ->
                    if (generation != listGeneration || _binding == null) return@onSuccess

                    if (apiList.isEmpty()) {
                        hasMore = false
                    } else {
                        val mapped = apiList.map { it.toDoctor() }
                        doctorsAdapter.appendDoctors(mapped)
                        currentOffset += apiList.size
                        if (apiList.size < PAGE_SIZE) {
                            hasMore = false
                        }
                    }

                    val count = doctorsAdapter.itemCount
                    updateResultsCount(count)
                    Log.d("DoctorsFragment", "Loaded more: +${apiList.size}, total shown=$count")
                }.onFailure { error ->
                    if (error is CancellationException) throw error
                    if (_binding == null) return@onFailure
                    Log.e("DoctorsFragment", "Error loading more doctors", error)
                    Snackbar.make(
                        binding.root,
                        "Не удалось подгрузить врачей: ${error.message}",
                        Snackbar.LENGTH_SHORT,
                    ).show()
                }
            } finally {
                isPagingLoading = false
                if (_binding != null) {
                    binding.loadMoreFooter.visibility = View.GONE
                }
            }
        }
    }

    private fun updateResultsCount(count: Int) {
        if (_binding == null) return
        binding.resultsCountTextView.text =
            when {
                count == 0 -> ""
                hasMore -> getString(R.string.doctors_loaded_has_more, count)
                else -> getString(R.string.doctors_loaded_count, count)
            }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (_binding == null) return
        if (isEmpty) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.recyclerViewDoctors.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.recyclerViewDoctors.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        searchJob = null
        refreshJob?.cancel()
        refreshJob = null
        _binding = null
    }
}

private fun DoctorApi.toDoctor(): Doctor {
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

    return Doctor(
        id = id.toString(),
        name = fullName.ifEmpty { user.email },
        specialty = specialtyText,
        rating = rating ?: 0.0,
        reviews = reviewCount ?: 0,
        experience = "$experienceYears лет",
        location = "",
        availability = "",
        image = photoUrl.toImageDataUri(),
        consultationFee = "",
    )
}
