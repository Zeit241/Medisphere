package com.example.kursovaya.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.kursovaya.databinding.ListItemReviewBinding
import com.example.kursovaya.model.Review

class ReviewsAdapter : ListAdapter<Review, ReviewsAdapter.ReviewViewHolder>(ReviewDiffCallback()) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ReviewViewHolder {
        val binding =
            ListItemReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ReviewViewHolder,
        position: Int,
    ) {
        holder.bind(getItem(position))
    }

    class ReviewViewHolder(private val binding: ListItemReviewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(review: Review) {
            binding.reviewerNameTextView.text = review.authorName
            binding.reviewRatingTextView.text = review.rating.toString()
            binding.reviewDateTextView.text = review.relativeTimeDescription
            binding.reviewCommentTextView.text = review.text
        }
    }
}

class ReviewDiffCallback : DiffUtil.ItemCallback<Review>() {
    override fun areItemsTheSame(
        oldItem: Review,
        newItem: Review,
    ): Boolean {
        // Используем уникальный ID отзыва, если он есть
        return if (oldItem.id != null && newItem.id != null) {
            oldItem.id == newItem.id
        } else {
            // Fallback на комбинацию имени и даты, если ID нет
            oldItem.authorName == newItem.authorName && oldItem.relativeTimeDescription == newItem.relativeTimeDescription
        }
    }

    override fun areContentsTheSame(
        oldItem: Review,
        newItem: Review,
    ): Boolean {
        return oldItem == newItem
    }
}
