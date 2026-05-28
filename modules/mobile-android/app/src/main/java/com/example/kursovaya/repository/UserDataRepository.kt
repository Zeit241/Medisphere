package com.example.kursovaya.repository

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.kursovaya.model.api.User
import com.google.gson.Gson
import com.google.gson.GsonBuilder

object UserDataRepository {
    private var cachedUser: User? = null
    private var sharedPreferences: EncryptedSharedPreferences? = null
    private val gson: Gson = GsonBuilder().create()

    fun init(context: Context) {
        if (sharedPreferences == null) {
            val masterKey =
                MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

            sharedPreferences =
                EncryptedSharedPreferences.create(
                    context,
                    "user_data_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                ) as EncryptedSharedPreferences
        }
    }

    fun saveUser(user: User) {
        cachedUser = user
        sharedPreferences?.edit()?.apply {
            val userJson = gson.toJson(user)
            putString("current_user", userJson)
            commit()
            Log.d("UserDataRepository", "Пользователь сохранен: ${user.email}")
        }
    }

    fun getCurrentUser(): User? {
        // Сначала проверяем кэш
        if (cachedUser != null) {
            return cachedUser
        }

        // Если нет в кэше, загружаем из SharedPreferences
        val userJson = sharedPreferences?.getString("current_user", null)
        return if (userJson != null) {
            try {
                cachedUser = gson.fromJson(userJson, User::class.java)
                Log.d("UserDataRepository", "Пользователь загружен из хранилища: ${cachedUser?.email}")
                cachedUser
            } catch (e: Exception) {
                Log.e("UserDataRepository", "Ошибка десериализации пользователя", e)
                null
            }
        } else {
            null
        }
    }

    fun clearUser() {
        cachedUser = null
        sharedPreferences?.edit()?.apply {
            remove("current_user")
            commit()
            Log.d("UserDataRepository", "Данные пользователя удалены")
        }
    }

    fun hasUser(): Boolean {
        return getCurrentUser() != null
    }
}
