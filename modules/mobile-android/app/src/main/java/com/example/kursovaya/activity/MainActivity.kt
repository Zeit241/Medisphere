package com.example.kursovaya.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.example.kursovaya.R
import com.example.kursovaya.auth.AuthSessionCoordinator
import com.example.kursovaya.model.AuthState
import com.example.kursovaya.repository.AuthRepository
import com.example.kursovaya.repository.UserDataRepository
import com.example.kursovaya.repository.UserRepository
import com.example.kursovaya.viewmodel.MainViewModel
import com.example.kursovaya.viewmodel.QueueSocketViewModel
import com.example.kursovaya.viewmodel.QueueSocketViewModelFactory
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class MainActivity : BaseActivity() {
    private val authRepository by lazy { AuthRepository(this) }
    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(authRepository) as T
            }
        }
    }

    private val queueSocketViewModel: QueueSocketViewModel by viewModels {
        QueueSocketViewModelFactory(application)
    }

    private lateinit var navController: NavController
    private lateinit var offlineOverlay: View
    private var currentRootTabId: Int = R.id.nav_home

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (authRepository.getAuthState() is AuthState.Unauthenticated) {
            navigateToLoginClearingTask()
            return
        }

        if (authRepository.isAccessTokenExpired()) {
            authRepository.clearAuth()
            navigateToLoginClearingTask()
            return
        }

        com.example.kursovaya.api.RetrofitClient.init(this)

        UserDataRepository.init(this)

        setContentView(R.layout.activity_main)

        offlineOverlay = findViewById(R.id.offlineOverlay)
        viewModel.startNetworkMonitor(this)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        var isNavigatingProgrammatically = false

        bottomNavigation.setOnItemSelectedListener { item ->
            if (isNavigatingProgrammatically) {
                return@setOnItemSelectedListener true
            }

            val selectedTabId = item.itemId

            val navOptions =
                NavOptions.Builder()
                    .setPopUpTo(navController.graph.startDestinationId, false)
                    .setLaunchSingleTop(true)
                    .build()

            try {
                navController.navigate(selectedTabId, null, navOptions)
                true
            } catch (e: IllegalArgumentException) {
                false
            }
        }

        bottomNavigation.setOnItemReselectedListener { item ->
            val selectedTabId = item.itemId
            navController.popBackStack(selectedTabId, false)
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            currentRootTabId = getRootDestinationId(destination.id)
            refreshOfflineOverlay()
            if (bottomNavigation.selectedItemId != currentRootTabId &&
                bottomNavigation.menu.findItem(currentRootTabId) != null
            ) {
                isNavigatingProgrammatically = true
                bottomNavigation.selectedItemId = currentRootTabId
                isNavigatingProgrammatically = false
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isOnline.collect { online ->
                        refreshOfflineOverlay()
                        syncQueueSocket(online)
                    }
                }
            }
        }

        loadUserData()
    }

    override fun onResume() {
        super.onResume()
        AuthSessionCoordinator.onSessionInvalidated = {
            if (!isFinishing) {
                queueSocketViewModel.disconnect()
                viewModel.logout()
                navigateToLoginClearingTask()
            }
        }

        if (authRepository.getAuthState() is AuthState.Authenticated && authRepository.isAccessTokenExpired()) {
            queueSocketViewModel.disconnect()
            viewModel.logout()
            navigateToLoginClearingTask()
        }
    }

    override fun onPause() {
        AuthSessionCoordinator.onSessionInvalidated = null
        super.onPause()
    }

    private fun navigateToLoginClearingTask() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun refreshOfflineOverlay() {
        if (!::offlineOverlay.isInitialized) return
        val online = viewModel.isOnline.value
        offlineOverlay.visibility =
            if (!online && currentRootTabId != R.id.nav_map) View.VISIBLE else View.GONE
    }

    private fun syncQueueSocket(online: Boolean) {
        val authed = authRepository.getAuthState() is AuthState.Authenticated
        if (online && authed) {
            queueSocketViewModel.connect()
        } else {
            queueSocketViewModel.disconnect()
        }
    }

    private fun getRootDestinationId(destinationId: Int): Int {
        val mainTabs =
            setOf(
                R.id.nav_home,
                R.id.nav_doctors,
                R.id.nav_ai_assistant,
                R.id.nav_queue,
                R.id.nav_map,
                R.id.nav_profile,
            )

        if (destinationId in mainTabs) {
            return destinationId
        }

        if (destinationId == R.id.appointmentHistoryFragment ||
            destinationId == R.id.appointmentDetailsFragment ||
            destinationId == R.id.editProfileFragment
        ) {
            return R.id.nav_profile
        }

        if (destinationId == R.id.bookingFragment) {
            val prev = navController.previousBackStackEntry?.destination?.id
            if (prev == R.id.nav_ai_assistant) return R.id.nav_ai_assistant
            return R.id.nav_doctors
        }
        if (destinationId == R.id.doctorProfileFragment ||
            destinationId == R.id.doctorsFragment
        ) {
            return R.id.nav_doctors
        }

        val graph = navController.graph
        for (tabId in mainTabs) {
            val tabNode = graph.findNode(tabId)
            if (tabNode != null) {
                if (tabNode is androidx.navigation.NavGraph) {
                    if (tabNode.findNode(destinationId) != null) {
                        return tabId
                    }
                }
                if (tabNode.id == destinationId) {
                    return tabId
                }
            }
        }

        return destinationId
    }

    private fun loadUserData() {
        lifecycleScope.launch {
            val cachedUser = UserDataRepository.getCurrentUser()
            if (cachedUser != null) {
                Log.d("MainActivity", "Используются кэшированные данные пользователя: ${cachedUser.email}")
            } else {
                Log.d("MainActivity", "Загрузка данных пользователя с сервера...")
                val userRepository = UserRepository(this@MainActivity)
                userRepository.getCurrentUser()
                    .onSuccess { user ->
                        Log.d("MainActivity", "Данные пользователя успешно загружены: ${user.email}")
                    }
                    .onFailure { error ->
                        Log.e("MainActivity", "Ошибка загрузки данных пользователя", error)
                    }
            }
        }
    }
}
