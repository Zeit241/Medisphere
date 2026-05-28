package com.example.kursovaya.model

import org.junit.Assert.*
import org.junit.Test

class DoctorProfileTest {
    @Test
    fun `DoctorProfile creation with all fields`() {
        val education =
            listOf(
                Education("Медицинский", "МГУ", "2010"),
            )
        val reviews =
            listOf(
                Review(
                    id = 1,
                    authorName = "Иван Иванов",
                    rating = 5,
                    relativeTimeDescription = "2 дня назад",
                    text = "Отличный врач",
                ),
            )

        val profile =
            DoctorProfile(
                id = "1",
                name = "Доктор Смирнов",
                specialty = "Терапевт",
                rating = 4.5,
                reviewCount = 10,
                experience = "10 лет",
                location = "Москва",
                availability = "Доступен",
                image = "image.jpg",
                consultationFee = "2000 руб",
                about = "Опытный врач",
                education = education,
                reviews = reviews,
            )

        assertEquals("1", profile.id)
        assertEquals("Доктор Смирнов", profile.name)
        assertEquals("Терапевт", profile.specialty)
        assertEquals(4.5, profile.rating, 0.01)
        assertEquals(10, profile.reviewCount)
        assertEquals(1, profile.education.size)
        assertEquals(1, profile.reviews.size)
    }

    @Test
    fun `DoctorProfile with empty lists`() {
        val profile =
            DoctorProfile(
                id = "2",
                name = "Доктор Петров",
                specialty = "Хирург",
                rating = 0.0,
                reviewCount = 0,
                experience = "5 лет",
                location = "СПб",
                availability = "Недоступен",
                image = "",
                consultationFee = "3000 руб",
                about = "",
                education = emptyList(),
                reviews = emptyList(),
            )

        assertTrue(profile.education.isEmpty())
        assertTrue(profile.reviews.isEmpty())
        assertEquals(0.0, profile.rating, 0.01)
        assertEquals(0, profile.reviewCount)
    }

    @Test
    fun `Review creation and equality`() {
        val review1 =
            Review(
                id = 1,
                authorName = "Иван",
                rating = 5,
                relativeTimeDescription = "1 день назад",
                text = "Отлично",
            )

        val review2 =
            Review(
                id = 1,
                authorName = "Иван",
                rating = 5,
                relativeTimeDescription = "1 день назад",
                text = "Отлично",
            )

        assertEquals(review1, review2)
        assertEquals(review1.hashCode(), review2.hashCode())
    }

    @Test
    fun `Education creation`() {
        val education =
            Education(
                degree = "Доктор наук",
                institution = "МГУ",
                year = "2010",
            )

        assertEquals("Доктор наук", education.degree)
        assertEquals("МГУ", education.institution)
        assertEquals("2010", education.year)
    }
}
