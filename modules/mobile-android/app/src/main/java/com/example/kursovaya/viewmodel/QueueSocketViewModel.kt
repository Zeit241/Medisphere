package com.example.kursovaya.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.kursovaya.model.websocket.QueueResponse
import com.example.kursovaya.model.websocket.QueueUpdate
import com.example.kursovaya.repository.AuthRepository
import com.example.kursovaya.websocket.QueueWebSocketClient
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class QueueSocketViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository(application)
    private val client = QueueWebSocketClient(authRepository)

    private val _queueResponses = MutableSharedFlow<QueueResponse>(extraBufferCapacity = 64)
    val queueResponses: SharedFlow<QueueResponse> = _queueResponses.asSharedFlow()

    private val _queueUpdates = MutableSharedFlow<QueueUpdate>(extraBufferCapacity = 64)
    val queueUpdates: SharedFlow<QueueUpdate> = _queueUpdates.asSharedFlow()

    private val _socketErrors = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val socketErrors: SharedFlow<String> = _socketErrors.asSharedFlow()

    private val _disconnectEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    val disconnectEvents: SharedFlow<Unit> = _disconnectEvents.asSharedFlow()

    init {
        client.setOnMessageCallback { response -> _queueResponses.tryEmit(response) }
        client.setOnQueueUpdateCallback { update -> _queueUpdates.tryEmit(update) }
        client.setOnConnectedCallback {
            client.getMyQueues()
        }
        client.setOnErrorCallback { t ->
            _socketErrors.tryEmit(t.message ?: t.toString())
        }
        client.setOnDisconnectedCallback {
            _disconnectEvents.tryEmit(Unit)
        }
    }

    fun connect() {
        client.connect()
    }

    fun disconnect() {
        client.disconnect()
    }

    fun getMyQueues() {
        client.getMyQueues()
    }

    fun subscribeToQueueUpdates(doctorId: Long) {
        client.subscribeToQueueUpdates(doctorId)
    }

    fun unsubscribeFromAllQueueUpdates() {
        client.unsubscribeFromAllQueueUpdates()
    }

    fun isConnected(): Boolean = client.isConnected()

    override fun onCleared() {
        client.disconnect()
        super.onCleared()
    }
}

class QueueSocketViewModelFactory(
    private val application: Application,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(QueueSocketViewModel::class.java)) {
            return QueueSocketViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
