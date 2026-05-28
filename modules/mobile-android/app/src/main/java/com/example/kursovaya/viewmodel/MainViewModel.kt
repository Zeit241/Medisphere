package com.example.kursovaya.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.kursovaya.model.AuthState
import com.example.kursovaya.network.NetworkConnectivityMonitor
import com.example.kursovaya.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private var networkMonitor: NetworkConnectivityMonitor? = null

    init {
        _authState.value = authRepository.getAuthState()
    }

    fun startNetworkMonitor(context: Context) {
        if (networkMonitor != null) return
        val app = context.applicationContext
        networkMonitor =
            NetworkConnectivityMonitor(app) { online ->
                _isOnline.value = online
            }.also { it.register() }
    }

    fun login(token: String) {
        authRepository.saveAuthToken(token)
        _authState.value = AuthState.Authenticated(token)
    }

    fun logout() {
        authRepository.clearAuth()
        _authState.value = AuthState.Unauthenticated
    }

    override fun onCleared() {
        networkMonitor?.unregister()
        networkMonitor = null
        super.onCleared()
    }
}
