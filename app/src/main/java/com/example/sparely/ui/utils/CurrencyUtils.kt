package com.example.sparely.ui.utils

/**
 * Filters the input string to allow only digits, dots, and commas.
 * Useful for currency input fields.
 */
fun String.filterCurrencyInput(): String {
    return this.filter { it.isDigit() || it == '.' || it == ',' }
}

/**
 * Safely converts a string to a Double, handling both dot and comma separators.
 * Replaces commas with dots before parsing.
 * Returns null if parsing fails.
 */
fun String.toSafeDouble(): Double? {
    return this.replace(',', '.').toDoubleOrNull()
}

/**
 * Safely converts a string to a Double, defaulting to 0.0 if parsing fails.
 */
fun String.toSafeDoubleOrZero(): Double {
    return this.toSafeDouble() ?: 0.0
}
