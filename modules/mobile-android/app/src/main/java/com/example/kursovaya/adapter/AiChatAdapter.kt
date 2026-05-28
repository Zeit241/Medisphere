package com.example.kursovaya.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kursovaya.R
import com.example.kursovaya.ai.parseAiAssistantJson
import com.example.kursovaya.databinding.ItemAiChatAssistantBinding
import com.example.kursovaya.databinding.ItemAiChatUserBinding
import com.example.kursovaya.model.api.toImageDataUri
import com.example.kursovaya.repository.AiChatTurn
import com.example.kursovaya.repository.DoctorsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

sealed class AiChatUiRow {
    data class User(val text: String) : AiChatUiRow()

    data class Assistant(
        val rawContent: String,
        val displayText: String,
        val doctorId: Long?,
        val serviceId: Long?,
        val serviceTitle: String?,
    ) : AiChatUiRow()
}

fun turnsToUiRows(
    turns: List<AiChatTurn>,
    serviceNameById: Map<Long, String>,
): List<AiChatUiRow> {
    return turns.map { t ->
        when (t.role) {
            "user" -> AiChatUiRow.User(t.content)
            else -> {
                val p = parseAiAssistantJson(t.content)
                val svcTitle =
                    p.serviceId?.let { id ->
                        serviceNameById[id]?.let { name ->
                            // строка форматируется во ViewHolder через ресурс при необходимости
                            name
                        }
                    }
                AiChatUiRow.Assistant(
                    rawContent = t.content,
                    displayText = p.text,
                    doctorId = p.doctorId,
                    serviceId = p.serviceId,
                    serviceTitle = svcTitle,
                )
            }
        }
    }
}

class AiChatAdapter(
    private val doctorsRepository: DoctorsRepository,
    private val lifecycleOwner: LifecycleOwner,
    private val onBook: (doctorId: Long, serviceId: Long?) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val items = mutableListOf<AiChatUiRow>()

    fun submitList(rows: List<AiChatUiRow>) {
        items.clear()
        items.addAll(rows)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int =
        when (items[position]) {
            is AiChatUiRow.User -> VIEW_USER
            is AiChatUiRow.Assistant -> VIEW_ASSISTANT
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_USER -> UserVH(ItemAiChatUserBinding.inflate(inflater, parent, false))
            else ->
                AssistantVH(
                    ItemAiChatAssistantBinding.inflate(inflater, parent, false),
                    doctorsRepository,
                    lifecycleOwner,
                    onBook,
                )
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        when (val row = items[position]) {
            is AiChatUiRow.User -> (holder as UserVH).bind(row)
            is AiChatUiRow.Assistant -> (holder as AssistantVH).bind(row)
        }
    }

    override fun getItemCount(): Int = items.size

    private class UserVH(private val binding: ItemAiChatUserBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AiChatUiRow.User) {
            binding.messageText.text = item.text
        }
    }

    private class AssistantVH(
        private val binding: ItemAiChatAssistantBinding,
        private val doctorsRepository: DoctorsRepository,
        private val lifecycleOwner: LifecycleOwner,
        private val onBook: (doctorId: Long, serviceId: Long?) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        private var doctorLoadJob: Job? = null

        fun bind(item: AiChatUiRow.Assistant) {
            doctorLoadJob?.cancel()
            binding.messageText.text = item.displayText

            val docId = item.doctorId
            binding.recommendationCard.isVisible = docId != null
            binding.bookButton.isVisible = docId != null

            if (docId == null) return

            val svc = item.serviceTitle
            binding.serviceName.isVisible = !svc.isNullOrBlank()
            binding.serviceName.text =
                binding.root.context.getString(
                    R.string.ai_assistant_service_line,
                    svc ?: "",
                )

            binding.doctorName.text = ""
            binding.doctorSpecialty.text = ""
            binding.doctorImage.setImageResource(R.drawable.placeholder_doctor)

            doctorLoadJob =
                lifecycleOwner.lifecycleScope.launch {
                    doctorsRepository.getDoctorById(docId).fold(
                        onSuccess = { api ->
                            if (bindingAdapterPosition == RecyclerView.NO_POSITION) return@fold
                            val fullName =
                                buildString {
                                    append(api.user.lastName)
                                    if (api.user.firstName.isNotEmpty()) append(" ${api.user.firstName}")
                                    if (!api.user.middleName.isNullOrBlank()) append(" ${api.user.middleName}")
                                }.trim().ifEmpty { api.user.email }
                            val spec =
                                api.specializations?.joinToString(", ") { it.name }
                                    ?: api.bio
                                    ?: ""
                            binding.doctorName.text = fullName
                            binding.doctorSpecialty.text = spec
                            val uri = api.photoUrl.toImageDataUri()
                            if (uri.isNotEmpty()) {
                                Glide.with(binding.doctorImage.context)
                                    .load(uri)
                                    .placeholder(R.drawable.placeholder_doctor)
                                    .error(R.drawable.placeholder_doctor)
                                    .into(binding.doctorImage)
                            } else {
                                binding.doctorImage.setImageResource(R.drawable.placeholder_doctor)
                            }
                        },
                        onFailure = {
                            if (bindingAdapterPosition == RecyclerView.NO_POSITION) return@fold
                            binding.doctorName.text =
                                binding.root.context.getString(R.string.ai_assistant_doctor_load_failed)
                            binding.doctorSpecialty.text = ""
                            binding.doctorImage.setImageResource(R.drawable.placeholder_doctor)
                        },
                    )
                }

            binding.bookButton.setOnClickListener {
                onBook(docId, item.serviceId)
            }
        }
    }

    companion object {
        private const val VIEW_USER = 0
        private const val VIEW_ASSISTANT = 1
    }
}
