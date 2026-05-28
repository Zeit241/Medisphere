package com.example.kursovaya

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun `basic math operations`() {
        assertEquals(10, 5 + 5)
        assertEquals(25, 5 * 5)
        assertEquals(2, 10 / 5)
        assertEquals(0, 10 % 5)
    }
}
