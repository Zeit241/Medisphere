package com.example.kursovaya.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.example.kursovaya.R
import com.example.kursovaya.auth.AuthSessionCoordinator
import com.example.kursovaya.fragment.QueueFragment
import com.example.kursovaya.model.AuthState
import com.example.kursovaya.network.NetworkConnectivityMonitor
import com.example.kursovaya.repository.AuthRepository
import com.example.kursovaya.repository.UserDataRepository
import com.example.kursovaya.viewmodel.QueueSocketViewModel
import com.example.kursovaya.viewmodel.QueueSocketViewModelFactory

class QueueActivity : BaseActivity() {
    private val authRepository by lazy { AuthRepository(this) }
    private lateinit var networkMonitor: NetworkConnectivityMonitor

    private val queueSocketViewModel: QueueSocketViewModel by viewModels {
        QueueSocketViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.kursovaya.api.RetrofitClient.init(this)
        UserDataRepository.init(this)
        setContentView(R.layout.activity_queue)

        networkMonitor =
            NetworkConnectivityMonitor(this) { online ->
                val authed = authRepository.getAuthState() is AuthState.Authenticated
                if (online && authed) {
                    queueSocketViewModel.connect()
                } else {
                    queueSocketViewModel.disconnect()
                }
            }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, QueueFragment())
                .commit()
        }
    }

    override fun onStart() {
        super.onStart()
        networkMonitor.register()
    }

    override fun onStop() {
        networkMonitor.unregister()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        AuthSessionCoordinator.onSessionInvalidated = {
            if (!isFinishing) {
                queueSocketViewModel.disconnect()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }

    override fun onPause() {
        AuthSessionCoordinator.onSessionInvalidated = null
        super.onPause()
    }
}
