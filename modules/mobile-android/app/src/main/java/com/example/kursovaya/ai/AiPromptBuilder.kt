package com.example.kursovaya.ai

import android.content.Context
import com.example.kursovaya.model.api.AiCatalogResponse
import com.google.gson.Gson

object AiPromptBuilder {
    private val gson = Gson()

    fun buildSystemPrompt(
        context: Context,
        catalog: AiCatalogResponse,
    ): String {
        val template = context.assets.open("ai_system_prompt_template.txt").bufferedReader().use { it.readText() }
        val doctorsJson = gson.toJson(catalog.doctors)
        val servicesJson = gson.toJson(catalog.services)
        return template
            .replace("{{СПИСОК_ВРАЧЕЙ}}", doctorsJson)
            .replace("{{СПИСОК_УСЛУГ}}", servicesJson)
    }
}
