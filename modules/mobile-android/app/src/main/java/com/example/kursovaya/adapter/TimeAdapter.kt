package com.example.kursovaya.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kursovaya.databinding.ListItemTimeBinding
import com.example.kursovaya.model.TimeItem
import com.google.android.material.card.MaterialCardView

class TimeAdapter(
    private val times: List<TimeItem>,
    private val onTimeSelected: (TimeItem) -> Unit,
) : RecyclerView.Adapter<TimeAdapter.TimeViewHolder>() {
    private var selectedPosition = RecyclerView.NO_POSITION

    init {
        selectedPosition = times.indexOfFirst { it.isSelected }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): TimeViewHolder {
        val binding =
            ListItemTimeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TimeViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: TimeViewHolder,
        position: Int,
    ) {
        holder.bind(times[position])
    }

    override fun getItemCount(): Int = times.size

    inner class TimeViewHolder(private val binding: ListItemTimeBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    if (selectedPosition == position) {
                        // Item is already selected, so deselect it.
                        times[position].isSelected = false
                        notifyItemChanged(position)
                        selectedPosition = RecyclerView.NO_POSITION
                    } else {
                        // Item is not selected, so select it.
                        // First, deselect the previously selected item.
                        if (selectedPosition != RecyclerView.NO_POSITION) {
                            times[selectedPosition].isSelected = false
                            notifyItemChanged(selectedPosition)
                        }
                        // Then, select the new item.
                        times[position].isSelected = true
                        notifyItemChanged(position)
                        selectedPosition = position
                    }
                    onTimeSelected(times[position])
                }
            }
        }

        fun bind(time: TimeItem) {
            binding.timeTextView.text = time.time
            (binding.root as MaterialCardView).isChecked = time.isSelected
            // Делаем занятые слоты неактивными
            binding.root.isEnabled = !time.isBooked
            binding.root.alpha = if (time.isBooked) 0.5f else 1.0f
        }
    }
}
