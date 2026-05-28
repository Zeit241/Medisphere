package com.example.kursovaya.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.kursovaya.adapter.ReviewsAdapter
import com.example.kursovaya.api.RetrofitClient
import com.example.kursovaya.databinding.FragmentReviewsBinding
import com.example.kursovaya.model.Review
import com.example.kursovaya.model.api.ReviewApi
import com.example.kursovaya.repository.DoctorsRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ReviewsFragment : Fragment() {
    private var _binding: FragmentReviewsBinding? = null
    private val binding get() = _binding!!

    private lateinit var reviewsAdapter: ReviewsAdapter
    private lateinit var doctorsRepository: DoctorsRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentReviewsBinding.inflate(inflater, container, false)
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
        setupRecyclerView()
        updateEmptyState(true) // Показываем пустое состояние до загрузки данных
        loadReviews()
    }

    private fun loadReviews() {
        val doctorId = arguments?.getString("DOCTOR_ID")
        if (doctorId == null) {
            reviewsAdapter.submitList(emptyList())
            return
        }

        val id = doctorId.toLongOrNull()
        if (id == null) {
            reviewsAdapter.submitList(emptyList())
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                doctorsRepository.getDoctorReviews(id)
                    .onSuccess { reviewsApi ->
                        if (_binding == null) return@onSuccess

                        val reviews = reviewsApi.map { it.toReview() }
                        reviewsAdapter.submitList(reviews)
                        updateEmptyState(reviews.isEmpty())

                        Log.d("ReviewsFragment", "Loaded reviews: ${reviews.size}")
                    }
                    .onFailure { error ->
                        if (_binding == null) return@launch
                        Log.e("ReviewsFragment", "Error loading reviews", error)
                        reviewsAdapter.submitList(emptyList())
                        updateEmptyState(true)
                    }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                if (_binding == null) return@launch
                Log.e("ReviewsFragment", "Unexpected error", e)
                reviewsAdapter.submitList(emptyList())
            }
        }
    }

    private fun setupRecyclerView() {
        reviewsAdapter = ReviewsAdapter()
        binding.reviewsRecyclerView.adapter = reviewsAdapter
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (_binding == null) return
        if (isEmpty) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.reviewsRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.reviewsRecyclerView.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Функция расширения для конвертации ReviewApi в Review
private fun ReviewApi.toReview(): Review {
    // Используем готовое имя пациента из API, если оно есть
    val authorName = patientName?.takeIf { it.isNotEmpty() } ?: "Анонимный пользователь"

    // Преобразуем дату в относительное время
    val relativeTime =
        try {
            // Пробуем разные форматы даты
            val formats =
                listOf(
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault()),
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()),
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
                )

            var reviewDate: Date? = null
            for (format in formats) {
                try {
                    reviewDate = format.parse(createdAt)
                    if (reviewDate != null) break
                } catch (e: Exception) {
                    // Пробуем следующий формат
                }
            }

            val date = reviewDate ?: Date()
            val now = Date()
            val diffInMillis = now.time - date.time
            val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)

            when {
                diffInDays == 0L -> "Сегодня"
                diffInDays == 1L -> "Вчера"
                diffInDays < 7 -> "$diffInDays ${getDayWord(diffInDays.toInt())} назад"
                diffInDays < 30 -> "${diffInDays / 7} ${getWeekWord((diffInDays / 7).toInt())} назад"
                diffInDays < 365 -> "${diffInDays / 30} ${getMonthWord((diffInDays / 30).toInt())} назад"
                else -> "${diffInDays / 365} ${getYearWord((diffInDays / 365).toInt())} назад"
            }
        } catch (e: Exception) {
            createdAt
        }

    return Review(
        id = id.toInt(),
        authorName = authorName,
        rating = rating,
        relativeTimeDescription = relativeTime,
        text = reviewText ?: "",
    )
}

private fun getDayWord(days: Int): String {
    return when {
        days % 10 == 1 && days % 100 != 11 -> "день"
        days % 10 in 2..4 && days % 100 !in 12..14 -> "дня"
        else -> "дней"
    }
}

private fun getWeekWord(weeks: Int): String {
    return when {
        weeks % 10 == 1 && weeks % 100 != 11 -> "неделю"
        weeks % 10 in 2..4 && weeks % 100 !in 12..14 -> "недели"
        else -> "недель"
    }
}

private fun getMonthWord(months: Int): String {
    return when {
        months % 10 == 1 && months % 100 != 11 -> "месяц"
        months % 10 in 2..4 && months % 100 !in 12..14 -> "месяца"
        else -> "месяцев"
    }
}

private fun getYearWord(years: Int): String {
    return when {
        years % 10 == 1 && years % 100 != 11 -> "год"
        years % 10 in 2..4 && years % 100 !in 12..14 -> "года"
        else -> "лет"
    }
}
