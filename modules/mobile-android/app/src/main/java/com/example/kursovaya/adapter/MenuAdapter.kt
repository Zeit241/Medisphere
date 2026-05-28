package com.example.kursovaya.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.kursovaya.databinding.ListItemMenuBinding
import com.example.kursovaya.model.MenuItem

class MenuAdapter : ListAdapter<MenuItem, MenuAdapter.MenuViewHolder>(MenuDiffCallback()) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): MenuViewHolder {
        val binding =
            ListItemMenuBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: MenuViewHolder,
        position: Int,
    ) {
        holder.bind(getItem(position))
    }

    class MenuViewHolder(private val binding: ListItemMenuBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MenuItem) {
            binding.menuIcon.setImageResource(item.iconRes)
            binding.menuLabel.text = item.label

            binding.root.setOnClickListener {
                item.action.invoke()
            }
        }
    }
}

class MenuDiffCallback : DiffUtil.ItemCallback<MenuItem>() {
    override fun areItemsTheSame(
        oldItem: MenuItem,
        newItem: MenuItem,
    ): Boolean {
        return oldItem.label == newItem.label
    }

    override fun areContentsTheSame(
        oldItem: MenuItem,
        newItem: MenuItem,
    ): Boolean {
        return oldItem == newItem
    }
}
