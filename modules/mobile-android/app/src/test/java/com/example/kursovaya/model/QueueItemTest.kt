package com.example.kursovaya.model

import org.junit.Assert.*
import org.junit.Test

class QueueItemTest {
    @Test
    fun `QueueItem creation with all fields`() {
        val queueItem =
            QueueItem(
                id = "1",
                doctorName = "Доктор Смирнов",
                specialty = "Терапевт",
                currentNumber = 5,
                yourNumber = 8,
                estimatedWait = "15 минут",
                status = "waiting",
                peopleAhead = 3,
                image = "doctor.jpg",
            )

        assertEquals("1", queueItem.id)
        assertEquals("Доктор Смирнов", queueItem.doctorName)
        assertEquals("Терапевт", queueItem.specialty)
        assertEquals(5, queueItem.currentNumber)
        assertEquals(8, queueItem.yourNumber)
        assertEquals("15 минут", queueItem.estimatedWait)
        assertEquals("waiting", queueItem.status)
        assertEquals(3, queueItem.peopleAhead)
        assertEquals("doctor.jpg", queueItem.image)
    }

    @Test
    fun `QueueItem with next status`() {
        val queueItem =
            QueueItem(
                id = "2",
                doctorName = "Доктор Петров",
                specialty = "Хирург",
                currentNumber = 10,
                yourNumber = 11,
                estimatedWait = "5 минут",
                status = "next",
                peopleAhead = 1,
                image = "doctor2.jpg",
            )

        assertEquals("next", queueItem.status)
        assertEquals(1, queueItem.peopleAhead)
    }

    @Test
    fun `QueueItem with ready status`() {
        val queueItem =
            QueueItem(
                id = "3",
                doctorName = "Доктор Иванов",
                specialty = "Кардиолог",
                currentNumber = 15,
                yourNumber = 15,
                estimatedWait = "0 минут",
                status = "ready",
                peopleAhead = 0,
                image = "doctor3.jpg",
            )

        assertEquals("ready", queueItem.status)
        assertEquals(0, queueItem.peopleAhead)
        assertEquals(15, queueItem.currentNumber)
        assertEquals(15, queueItem.yourNumber)
    }

    @Test
    fun `QueueItem equality test`() {
        val item1 =
            QueueItem(
                id = "1",
                doctorName = "Доктор",
                specialty = "Терапевт",
                currentNumber = 5,
                yourNumber = 8,
                estimatedWait = "15 минут",
                status = "waiting",
                peopleAhead = 3,
                image = "image.jpg",
            )

        val item2 =
            QueueItem(
                id = "1",
                doctorName = "Доктор",
                specialty = "Терапевт",
                currentNumber = 5,
                yourNumber = 8,
                estimatedWait = "15 минут",
                status = "waiting",
                peopleAhead = 3,
                image = "image.jpg",
            )

        assertEquals(item1, item2)
        assertEquals(item1.hashCode(), item2.hashCode())
    }
}
