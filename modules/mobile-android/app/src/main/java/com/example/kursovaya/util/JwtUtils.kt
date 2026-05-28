package com.example.kursovaya.util

import android.util.Base64
import org.json.JSONObject
import java.nio.charset.StandardCharsets

object JwtUtils {
    fun getExpirationUnixSeconds(token: String): Long? {
        val parts = token.split('.')
        if (parts.size < 2) return null
        val payloadJson = decodeBase64Url(parts[1]) ?: return null
        return try {
            val json = JSONObject(payloadJson)
            if (json.has("exp")) json.getLong("exp") else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * @return true только если удалось прочитать [exp] и срок истёк (с учётом [leewaySeconds]).
     * Нестандартный токен без exp не считается просроченным.
     */
    fun isExpired(
        token: String,
        leewaySeconds: Long = 60,
    ): Boolean {
        val exp = getExpirationUnixSeconds(token) ?: return false
        val nowSec = System.currentTimeMillis() / 1000
        return nowSec >= exp - leewaySeconds
    }

    private fun decodeBase64Url(segment: String): String? {
        return try {
            val padded = segment + "=".repeat((4 - segment.length % 4) % 4)
            val decoded = Base64.decode(padded, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            String(decoded, StandardCharsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }
}
