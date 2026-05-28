package com.example.kursovaya.auth

import android.os.Handler
import android.os.Looper
import com.example.kursovaya.model.AuthState
import com.example.kursovaya.repository.AuthRepository

/**
 * Реакция на 401 и принудительный выход: очистка сессии на главном потоке и колбэк в [MainActivity].
 */
object AuthSessionCoordinator {
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    var onSessionInvalidated: (() -> Unit)? = null

    fun notifyUnauthorized(context: android.content.Context) {
        val app = context.applicationContext
        mainHandler.post {
            synchronized(this) {
                val repo = AuthRepository(app)
                if (repo.getAuthState() is AuthState.Unauthenticated) return@post
                repo.clearAuth()
            }
            onSessionInvalidated?.invoke()
        }
    }
}
