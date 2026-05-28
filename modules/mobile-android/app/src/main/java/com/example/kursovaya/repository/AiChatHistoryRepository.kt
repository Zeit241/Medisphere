package com.example.kursovaya.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/** Сообщения для API (без system — он подставляется при отправке). */
data class AiChatTurn(
    val role: String,
    val content: String,
)

class AiChatHistoryRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun loadTurns(): MutableList<AiChatTurn> {
        val json = prefs.getString(KEY_TURNS, null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<AiChatTurn>>() {}.type
        return try {
            gson.fromJson(json, type) ?: mutableListOf()
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    fun saveTurns(turns: List<AiChatTurn>) {
        prefs.edit().putString(KEY_TURNS, gson.toJson(turns)).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_TURNS).apply()
    }

    companion object {
        private const val PREFS = "ai_assistant_chat"
        private const val KEY_TURNS = "turns"
    }
}
