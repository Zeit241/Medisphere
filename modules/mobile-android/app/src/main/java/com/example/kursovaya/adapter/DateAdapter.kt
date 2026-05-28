package com.example.kursovaya.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kursovaya.databinding.ListItemDateBinding
import com.example.kursovaya.model.DateItem
import com.google.android.material.card.MaterialCardView

class DateAdapter(
    private val dates: List<DateItem>,
    private val onDateSelected: (DateItem) -> Unit,
) : RecyclerView.Adapter<DateAdapter.DateViewHolder>() {
    private var selectedPosition = RecyclerView.NO_POSITION

    init {
        selectedPosition = dates.indexOfFirst { it.isSelected }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): DateViewHolder {
        val binding =
            ListItemDateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DateViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: DateViewHolder,
        position: Int,
    ) {
        holder.bind(dates[position])
    }

    override fun getItemCount(): Int = dates.size

    inner class DateViewHolder(private val binding: ListItemDateBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    if (selectedPosition == position) {
                        // Item is already selected, so deselect it.
                        dates[position].isSelected = false
                        notifyItemChanged(position)
                        selectedPosition = RecyclerView.NO_POSITION
                    } else {
                        // Item is not selected, so select it.
                        // First, deselect the previously selected item.
                        if (selectedPosition != RecyclerView.NO_POSITION) {
                            dates[selectedPosition].isSelected = false
                            notifyItemChanged(selectedPosition)
                        }
                        // Then, select the new item.
                        dates[position].isSelected = true
                        notifyItemChanged(position)
                        selectedPosition = position
                    }
                    onDateSelected(dates[position])
                }
            }
        }

        fun bind(date: DateItem) {
            binding.dayOfWeekTextView.text = date.dayOfWeek
            binding.dayOfMonthTextView.text = date.dayOfMonth
            binding.monthTextView.text = date.month
            (binding.root as MaterialCardView).isChecked = date.isSelected
        }
    }
}
