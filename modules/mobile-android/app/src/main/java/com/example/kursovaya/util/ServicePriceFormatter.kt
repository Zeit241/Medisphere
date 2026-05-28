package com.example.kursovaya.util

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object ServicePriceFormatter {
    /** Форматирует цену в рублях или возвращает null, если показывать сумму нечего */
    fun formatRub(price: Double?): String? {
        if (price == null || price <= 0 || price.isNaN()) return null
        return try {
            val fmt =
                NumberFormat.getCurrencyInstance(Locale("ru", "RU")).apply {
                    maximumFractionDigits = 0
                    currency = Currency.getInstance("RUB")
                }
            fmt.format(price)
        } catch (_: Exception) {
            String.format(Locale("ru", "RU"), "%.0f ₽", price)
        }
    }
}
