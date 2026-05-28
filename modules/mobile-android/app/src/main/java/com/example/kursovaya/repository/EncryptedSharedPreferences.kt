package com.example.kursovaya.repository
import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.kursovaya.model.AuthState
import com.example.kursovaya.util.JwtUtils

class AuthRepository(context: Context) {
    private val masterKey =
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

    private val sharedPreferences =
        EncryptedSharedPreferences.create(
            context,
            "auth_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

    fun getAuthState(): AuthState {
        val token = sharedPreferences.getString("access_token", null)
        return if (!token.isNullOrBlank()) {
            AuthState.Authenticated(token)
        } else {
            AuthState.Unauthenticated
        }
    }

    fun saveAuthToken(token: String) {
        sharedPreferences.edit()
            .putString("access_token", token)
            .commit() // Используем commit() для синхронного сохранения
    }

    fun clearAuth() {
        sharedPreferences.edit()
            .remove("access_token")
            .apply()
        // Очищаем данные пользователя при выходе
        UserDataRepository.clearUser()
    }

    /** true, если JWT с полем exp истёк (иначе false, в т.ч. при нестандартном токене). */
    fun isAccessTokenExpired(leewaySeconds: Long = 60): Boolean {
        val token = sharedPreferences.getString("access_token", null) ?: return false
        if (token.isBlank()) return false
        return JwtUtils.isExpired(token, leewaySeconds)
    }
}
