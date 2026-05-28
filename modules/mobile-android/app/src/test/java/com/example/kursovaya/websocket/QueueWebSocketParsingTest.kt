package com.example.kursovaya.websocket

import com.example.kursovaya.model.websocket.PositionData
import com.example.kursovaya.model.websocket.QueueEntry
import com.example.kursovaya.model.websocket.QueueResponse
import com.example.kursovaya.model.websocket.QueueUpdate
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueWebSocketParsingTest {
    private val gson = Gson()

    @Test
    fun parseQueueResponse_withQueueEntries() {
        val json =
            """
            {"success":true,"message":"ok","data":[{"doctorId":3,"patientId":4,"position":2,"lastUpdated":"2026-01-01T00:00:00Z"}]}
            """.trimIndent()

        val response = gson.fromJson(json, QueueResponse::class.java)

        assertTrue(response.success)
        assertEquals("ok", response.message)
        val entries =
            gson.fromJson(
                gson.toJson(response.data),
                object : TypeToken<List<QueueEntry>>() {}.type,
            ) as List<QueueEntry>
        assertEquals(2, entries.first().position)
    }

    @Test
    fun parseQueueUpdate_payload() {
        val json =
            """
            {"doctorId":3,"queue":[{"doctorId":3,"patientId":4,"position":1,"lastUpdated":"2026-01-01T00:00:00Z"}]}
            """.trimIndent()

        val update = gson.fromJson(json, QueueUpdate::class.java)

        assertEquals(3L, update.doctorId)
        assertEquals(1, update.queue.size)
        assertEquals(4L, update.queue.first().patientId)
    }

    @Test
    fun parsePositionData_payload() {
        val json =
            """
            {"queueEntryId":1,"doctorId":3,"patientId":4,"position":1,"isNext":true,"message":"You are next"}
            """.trimIndent()

        val position = gson.fromJson(json, PositionData::class.java)

        assertTrue(position.isNext)
        assertEquals("You are next", position.message)
    }
}
