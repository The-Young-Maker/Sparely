package com.example.sparely.ui.utils

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Ensures a [LocalDate] is within the Material 3 DatePicker's valid range (1900-2100).
 * Returns the epoch milli in UTC for use with [rememberDatePickerState].
 */
fun LocalDate?.toSafeDatePickerMillis(): Long? {
    if (this == null) return null
    return if (this.year in 1900..2100) {
        this.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    } else {
        // Fallback to now if corrupted (e.g. year 0002)
        LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
}

/**
 * Ensures a [LocalDateTime] is within the Material 3 DatePicker's valid range.
 */
fun LocalDateTime?.toSafeDatePickerMillis(): Long {
    val date = this?.toLocalDate() ?: LocalDate.now()
    return if (date.year in 1900..2100) {
        this?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli() 
            ?: LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    } else {
        LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
}
