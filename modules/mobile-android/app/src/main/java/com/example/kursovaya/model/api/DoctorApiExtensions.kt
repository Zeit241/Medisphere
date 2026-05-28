package com.example.kursovaya.model.api

import com.example.kursovaya.BuildConfig
import java.net.URLEncoder

/**
 * На эмуляторе `localhost` в URL указывает на сам эмулятор, а не на ПК с Docker/Directus.
 * Те же хост/порт, что и для Retrofit (10.0.2.2 → loopback ПК). В release не трогаем.
 */
fun String.rewriteLoopbackForEmulatorIfNeeded(): String {
    if (!BuildConfig.REWRITE_LOOPBACK_IN_IMAGE_URLS) return this
    val h = BuildConfig.LOOPBACK_REPLACEMENT_HOST
    return this
        .replace("://localhost:", "://$h:")
        .replace("://127.0.0.1:", "://$h:")
}

/**
 * Glide не шлёт Authorization; приватные файлы Directus отдают 403 без [access_token] в query.
 * Токен — из local.properties `directus.static.token` (тот же static token, что в админке Vite).
 */
fun String.appendDirectusAssetAccessTokenIfConfigured(): String {
    val t = BuildConfig.DIRECTUS_STATIC_TOKEN
    if (t.isBlank() || !contains("/assets/")) return this
    if (contains("access_token=")) return this
    val sep = if (contains("?")) "&" else "?"
    return this + sep + "access_token=" + URLEncoder.encode(t, Charsets.UTF_8.name())
}

/**
 * Конвертирует base64 строку в data URI для использования с Glide
 * Если строка уже является валидным URL (начинается с http:// или https://), возвращает её без изменений
 * Если строка является base64 (начинается с "/9j/" для JPEG), добавляет префикс data URI
 */
fun String?.toImageDataUri(): String {
    if (this.isNullOrEmpty()) return ""

    // Data URI — как есть
    if (startsWith("data:image")) {
        return this
    }
    // HTTP(S): localhost → 10.0.2.2 + access_token для /assets (иначе 403 у Directus)
    if (startsWith("http://") || startsWith("https://")) {
        return rewriteLoopbackForEmulatorIfNeeded().appendDirectusAssetAccessTokenIfConfigured()
    }

    // Если это base64 JPEG (начинается с "/9j/"), добавляем префикс
    if (startsWith("/9j/") || startsWith("iVBORw0KGgo")) {
        // Определяем тип изображения по префиксу
        val mimeType =
            when {
                startsWith("/9j/") -> "image/jpeg"
                startsWith("iVBORw0KGgo") -> "image/png"
                else -> "image/jpeg" // По умолчанию JPEG
            }
        return "data:$mimeType;base64,$this"
    }

    // Если строка не пустая, но не распознана, пробуем как base64 JPEG
    return "data:image/jpeg;base64,$this"
}
