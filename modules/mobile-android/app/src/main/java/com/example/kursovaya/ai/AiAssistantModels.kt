package com.example.kursovaya.ai

import com.google.gson.JsonElement
import com.google.gson.JsonParser

data class AiStructuredReply(
    val text: String,
    val doctorId: Long?,
    val serviceId: Long?,
)

fun parseAiAssistantJson(raw: String): AiStructuredReply {
    val cleaned =
        raw.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    return try {
        val obj = JsonParser.parseString(cleaned).asJsonObject
        val text = elementToString(obj.get("text")) ?: cleaned
        val doctorId = elementToLong(obj.get("doctorId"))
        val serviceId = elementToLong(obj.get("serviceId"))
        AiStructuredReply(text, doctorId, serviceId)
    } catch (_: Exception) {
        AiStructuredReply(cleaned, null, null)
    }
}

private fun elementToString(el: JsonElement?): String? {
    if (el == null || el.isJsonNull) return null
    return try {
        if (el.isJsonPrimitive) el.asJsonPrimitive.asString else null
    } catch (_: Exception) {
        null
    }
}

private fun elementToLong(el: JsonElement?): Long? {
    if (el == null || el.isJsonNull) return null
    return try {
        val p = el.asJsonPrimitive
        when {
            p.isNumber -> p.asLong
            p.isString -> p.asString.toLongOrNull()
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}
