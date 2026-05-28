package com.example.kursovaya.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.kursovaya.R // Убедитесь, что этот импорт правильный
import com.example.kursovaya.databinding.ListItemQueueBinding
import com.example.kursovaya.model.QueueItem

// Убедитесь, что класс QueueItem определен в отдельном файле или вне других классов.
// Например, в файле QueueItem.kt
// data class QueueItem(...)

class QueueAdapter : ListAdapter<QueueItem, QueueAdapter.QueueViewHolder>(QueueDiffCallback()) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): QueueViewHolder {
        val binding =
            ListItemQueueBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return QueueViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: QueueViewHolder,
        position: Int,
    ) {
        holder.bind(getItem(position))
    }

    class QueueViewHolder(private val binding: ListItemQueueBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: QueueItem) {
            binding.doctorNameTextView.text = item.doctorName
            binding.doctorSpecialtyTextView.text = item.specialty
            binding.currentNumberTextView.text = item.currentNumber.toString()
            binding.yourNumberTextView.text = item.yourNumber.toString()
            binding.estimatedWaitChip.text = item.estimatedWait
            // Исправляем отображение количества людей
            binding.peopleAheadTextView.text =
                "${item.peopleAhead} ${if (item.peopleAhead == 1) "человек впереди" else "человек впереди"}"

            val progress =
                if (item.yourNumber <= item.currentNumber) 100 else (item.currentNumber.toFloat() / (item.yourNumber - 1).toFloat() * 100).toInt()
            binding.queueProgressIndicator.progress = progress

            // Установка статуса
            val context = binding.root.context
            when (item.status) {
                "next" -> {
                    // Используем ресурсы для цветов, а не жестко заданные значения
//                    binding.statusBanner.setBackgroundColor(context.getColor(R.color.yellow_light))
//                    binding.statusIcon.setImageResource(R.drawable.ic_alert_circle)
                    binding.statusTextView.text = "Следующий в очереди"
                }

                "ready" -> {
//                    binding.statusBanner.setBackgroundColor(context.getColor(R.color.green_light))
//                    binding.statusIcon.setImageResource(R.drawable.ic_check_circle)
                    binding.statusTextView.text = "Ваша очередь"
                }

                else -> { // "waiting"
//                    binding.statusBanner.setBackgroundColor(context.getColor(R.color.grey_light))
                    binding.statusIcon.setImageResource(R.drawable.ic_clock)
                    binding.statusTextView.text = "В ожидании"
                }
            }
            // Здесь должна быть логика загрузки изображений, например, с помощью Glide
            // val imageResId = context.resources.getIdentifier(item.image, "drawable", context.packageName)
            // binding.doctorImageView.setImageResource(imageResId)
        }
    }
}

// Исправленный QueueDiffCallback
class QueueDiffCallback : DiffUtil.ItemCallback<QueueItem>() {
    override fun areItemsTheSame(
        oldItem: QueueItem,
        newItem: QueueItem,
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: QueueItem,
        newItem: QueueItem,
    ): Boolean {
        return oldItem == newItem
    }
}
