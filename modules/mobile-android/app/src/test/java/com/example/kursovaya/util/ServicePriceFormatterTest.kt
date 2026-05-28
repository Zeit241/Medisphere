package com.example.kursovaya.util

import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServicePriceFormatterTest {
    @Test
    fun formatRub_returnsNullForNull() {
        assertNull(ServicePriceFormatter.formatRub(null))
    }

    @Test
    fun formatRub_returnsNullForZeroOrNegative() {
        assertNull(ServicePriceFormatter.formatRub(0.0))
        assertNull(ServicePriceFormatter.formatRub(-10.0))
    }

    @Test
    fun formatRub_formatsPositiveAmount() {
        val formatted = ServicePriceFormatter.formatRub(1500.0)
        assertTrue(formatted!!.contains("1"))
        assertTrue(formatted.contains("500") || formatted.contains("1 500") || formatted.contains("1 500"))
    }

    @Test
    fun formatRub_returnsNullForNaN() {
        assertNull(ServicePriceFormatter.formatRub(Double.NaN))
    }
}
