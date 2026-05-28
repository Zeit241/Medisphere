package com.example.kursovaya.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kursovaya.R
import com.example.kursovaya.model.Doctor

class DoctorsAdapter(
    doctors: List<Doctor>,
    private val onDoctorClicked: (Doctor) -> Unit,
    private val onBookClicked: (Doctor) -> Unit,
) : RecyclerView.Adapter<DoctorsAdapter.DoctorViewHolder>() {
    private val doctors = doctors.toMutableList()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): DoctorViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_doctor_detailed, parent, false)
        return DoctorViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: DoctorViewHolder,
        position: Int,
    ) {
        holder.bind(doctors[position], onDoctorClicked, onBookClicked)
    }

    override fun getItemCount(): Int = doctors.size

    fun updateDoctors(newDoctors: List<Doctor>) {
        doctors.clear()
        doctors.addAll(newDoctors)
        notifyDataSetChanged()
    }

    fun appendDoctors(more: List<Doctor>) {
        if (more.isEmpty()) return
        val start = doctors.size
        doctors.addAll(more)
        notifyItemRangeInserted(start, more.size)
    }

    class DoctorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.doctorNameTextView)
        private val specialtyTextView: TextView =
            itemView.findViewById(R.id.doctorSpecialtyTextView)
        private val ratingTextView: TextView = itemView.findViewById(R.id.doctorRatingTextView)
        private val reviewsTextView: TextView = itemView.findViewById(R.id.doctorReviewsTextView)
        private val bookButton: Button = itemView.findViewById(R.id.bookButton)
        private val doctorImageView: ImageView = itemView.findViewById(R.id.doctorImageView)

        fun bind(
            doctor: Doctor,
            onDoctorClicked: (Doctor) -> Unit,
            onBookClicked: (Doctor) -> Unit,
        ) {
            nameTextView.text = doctor.name
            specialtyTextView.text = doctor.specialty
            ratingTextView.text = doctor.rating.toString()
            reviewsTextView.text = "(${doctor.reviews})"

            // Загружаем изображение врача
            if (doctor.image.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(doctor.image)
                    .placeholder(R.drawable.bg_circle_primary)
                    .error(R.drawable.bg_circle_primary)
                    .circleCrop()
                    .into(doctorImageView)
            } else {
                doctorImageView.setImageResource(R.drawable.bg_circle_primary)
            }

            itemView.setOnClickListener { onDoctorClicked(doctor) }
            bookButton.setOnClickListener { onBookClicked(doctor) }
        }
    }
}
