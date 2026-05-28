package com.example.kursovaya.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kursovaya.R
import com.example.kursovaya.adapter.AiChatAdapter
import com.example.kursovaya.adapter.turnsToUiRows
import com.example.kursovaya.ai.AiPromptBuilder
import com.example.kursovaya.ai.QwenChatRepository
import com.example.kursovaya.databinding.FragmentAiAssistantBinding
import com.example.kursovaya.model.api.AiCatalogResponse
import com.example.kursovaya.repository.AiChatHistoryRepository
import com.example.kursovaya.repository.AiChatTurn
import com.example.kursovaya.repository.CatalogRepository
import com.example.kursovaya.repository.DoctorsRepository
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class AiAssistantFragment : Fragment() {
    private var _binding: FragmentAiAssistantBinding? = null
    private val binding get() = _binding!!

    private lateinit var historyRepository: AiChatHistoryRepository
    private lateinit var catalogRepository: CatalogRepository
    private lateinit var doctorsRepository: DoctorsRepository
    private val qwenRepository = QwenChatRepository()

    private var chatAdapter: AiChatAdapter? = null
    private var systemPrompt: String? = null
    private var aiCatalog: AiCatalogResponse? = null
    private val turns = mutableListOf<AiChatTurn>()
    private var catalogReady: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAiAssistantBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        historyRepository = AiChatHistoryRepository(requireContext())
        catalogRepository = CatalogRepository(requireContext())
        doctorsRepository = DoctorsRepository(requireContext())

        turns.clear()
        turns.addAll(historyRepository.loadTurns())

        setChatInputEnabled(false)

        val adapter =
            AiChatAdapter(doctorsRepository, viewLifecycleOwner) { doctorId, serviceId ->
                val bundle =
                    Bundle().apply {
                        putString("DOCTOR_ID", doctorId.toString())
                        serviceId?.let { putString("SERVICE_ID", it.toString()) }
                    }
                findNavController().navigate(R.id.bookingFragment, bundle)
            }
        chatAdapter = adapter
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.chatRecyclerView.adapter = adapter

        binding.toolbar.setOnMenuItemClickListener(::onMenuItemSelected)

        binding.sendFab.setOnClickListener { sendMessage() }
        binding.messageInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }

        refreshUiFromTurns()
        loadCatalog()
    }

    private fun setChatInputEnabled(enabled: Boolean) {
        catalogReady = enabled
        binding.messageInput.isEnabled = enabled
        binding.sendFab.isEnabled = enabled
        binding.messageInputLayout.isEnabled = enabled
    }

    private fun onMenuItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_clear_chat -> {
                turns.clear()
                historyRepository.clear()
                refreshUiFromTurns()
                return true
            }
            R.id.action_refresh_catalog -> {
                loadCatalog()
                return true
            }
        }
        return false
    }

    private fun serviceNameMap(): Map<Long, String> = aiCatalog?.services?.associate { it.id to it.name }.orEmpty()

    private fun refreshUiFromTurns() {
        chatAdapter?.submitList(turnsToUiRows(turns, serviceNameMap()))
        binding.chatRecyclerView.post {
            val n = chatAdapter?.itemCount ?: 0
            if (n > 0) binding.chatRecyclerView.smoothScrollToPosition(n - 1)
        }
    }

    private fun loadCatalog() {
        viewLifecycleOwner.lifecycleScope.launch {
            catalogRepository.getAiReferenceCatalog()
                .onSuccess { catalog ->
                    val b = _binding ?: return@onSuccess
                    aiCatalog = catalog
                    systemPrompt = AiPromptBuilder.buildSystemPrompt(requireContext(), catalog)
                    b.catalogStatusTextView.text =
                        getString(R.string.ai_assistant_catalog_ok)
                    setChatInputEnabled(true)
                    refreshUiFromTurns()
                }
                .onFailure {
                    systemPrompt = null
                    aiCatalog = null
                    val b = _binding ?: return@onFailure
                    b.catalogStatusTextView.text =
                        getString(R.string.ai_assistant_chat_unavailable)
                    setChatInputEnabled(false)
                    refreshUiFromTurns()
                }
        }
    }

    private fun sendMessage() {
        val text = binding.messageInput.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) {
            Snackbar.make(binding.root, R.string.ai_assistant_empty_input, Snackbar.LENGTH_SHORT)
                .show()
            return
        }
        val prompt = systemPrompt
        if (prompt == null) {
            Snackbar.make(binding.root, R.string.ai_assistant_chat_unavailable, Snackbar.LENGTH_LONG)
                .show()
            return
        }

        binding.messageInput.setText("")
        turns.add(AiChatTurn("user", text))
        historyRepository.saveTurns(turns)
        refreshUiFromTurns()

        val messages =
            buildList {
                add("system" to prompt)
                turns.forEach { add(it.role to it.content) }
            }

        binding.sendProgress.isVisible = true
        binding.sendFab.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            qwenRepository.chatCompletion(messages = messages)
                .onSuccess { raw ->
                    if (_binding == null) return@onSuccess
                    turns.add(AiChatTurn("assistant", raw))
                    historyRepository.saveTurns(turns)
                    refreshUiFromTurns()
                }
                .onFailure { e ->
                    val root = _binding?.root ?: return@onFailure
                    Snackbar.make(
                        root,
                        getString(R.string.ai_assistant_qwen_error) + "\n${e.message}",
                        Snackbar.LENGTH_LONG,
                    ).show()
                }
            _binding?.let { b ->
                b.sendProgress.isVisible = false
                b.sendFab.isEnabled = catalogReady
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        chatAdapter = null
        _binding = null
    }
}
