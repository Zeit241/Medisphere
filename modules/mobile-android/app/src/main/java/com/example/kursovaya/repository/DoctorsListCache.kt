package com.example.kursovaya.repository

import com.example.kursovaya.model.api.DoctorApi
import java.util.concurrent.ConcurrentHashMap

/**
 * Кэш страниц списка врачей в памяти (TTL 5 мин), ключ: запрос + limit + offset.
 */
internal object DoctorsListCache {
    private const val TTL_MS = 5 * 60_000L

    private data class Entry(val data: List<DoctorApi>, val expiresAt: Long)

    private val map = ConcurrentHashMap<String, Entry>()

    private fun key(
        query: String?,
        limit: Int,
        offset: Int,
    ) = "${query ?: ""}\t$limit\t$offset"

    fun get(
        query: String?,
        limit: Int,
        offset: Int,
    ): List<DoctorApi>? {
        val k = key(query, limit, offset)
        val e = map[k] ?: return null
        if (System.currentTimeMillis() > e.expiresAt) {
            map.remove(k)
            return null
        }
        return e.data
    }

    fun put(
        query: String?,
        limit: Int,
        offset: Int,
        data: List<DoctorApi>,
    ) {
        map[key(query, limit, offset)] = Entry(data, System.currentTimeMillis() + TTL_MS)
    }

    fun invalidateAll() {
        map.clear()
    }
}
