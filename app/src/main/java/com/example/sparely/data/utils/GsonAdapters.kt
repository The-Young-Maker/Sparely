package com.example.sparely.data.utils

import com.google.gson.*
import java.lang.reflect.Type
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class LocalDateAdapter : JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
    override fun serialize(src: LocalDate?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return if (src == null) JsonNull.INSTANCE else JsonPrimitive(src.toString())
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): LocalDate {
        if (json == null || json.isJsonNull) throw JsonParseException("Null LocalDate")
        if (json.isJsonPrimitive) {
            return LocalDate.parse(json.asString)
        }
        if (json.isJsonObject) {
            val obj = json.asJsonObject
            if (obj.size() == 0) return LocalDate.of(1970, 1, 1) // Fallback for empty object
            val year = obj.get("year")?.asInt
            val month = obj.get("month")?.asInt
            val day = obj.get("day")?.asInt
            if (year != null && month != null && day != null) {
                return LocalDate.of(year, month, day)
            }
        }
        throw JsonParseException("Cannot parse LocalDate from $json")
    }
}

class LocalDateTimeAdapter : JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
    override fun serialize(src: LocalDateTime?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return if (src == null) JsonNull.INSTANCE else JsonPrimitive(src.toString())
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): LocalDateTime {
        if (json == null || json.isJsonNull) throw JsonParseException("Null LocalDateTime")
        if (json.isJsonPrimitive) {
            return LocalDateTime.parse(json.asString)
        }
        if (json.isJsonObject) {
            val obj = json.asJsonObject
            if (obj.size() == 0) return LocalDateTime.of(1970, 1, 1, 0, 0) // Fallback
            // Handle default Gson serialization which might nest date and time or be flat depending on JDK
            // Default Gson often serializes LocalDateTime as {date: {year: ...}, time: {hour: ...}}
            if (obj.has("date") && obj.has("time")) {
                 val dateObj = obj.getAsJsonObject("date")
                 val timeObj = obj.getAsJsonObject("time")
                 
                 val year = dateObj.get("year")?.asInt
                 val month = dateObj.get("month")?.asInt
                 val day = dateObj.get("day")?.asInt
                 
                 val hour = timeObj.get("hour")?.asInt
                 val minute = timeObj.get("minute")?.asInt
                 val second = timeObj.get("second")?.asInt ?: 0
                 val nano = timeObj.get("nano")?.asInt ?: 0
                 
                 if (year != null && month != null && day != null && hour != null && minute != null) {
                     return LocalDateTime.of(year, month, day, hour, minute, second, nano)
                 }
            } else if (obj.has("year") && obj.has("month") && obj.has("day")) {
                 // Try flat format
                 val year = obj.get("year")?.asInt
                 val month = obj.get("month")?.asInt
                 val day = obj.get("day")?.asInt
                 val hour = obj.get("hour")?.asInt ?: 0
                 val minute = obj.get("minute")?.asInt ?: 0
                 val second = obj.get("second")?.asInt ?: 0
                 val nano = obj.get("nano")?.asInt ?: 0

                 if (year != null && month != null && day != null) {
                     return LocalDateTime.of(year, month, day, hour, minute, second, nano)
                 }
            }
        }
        throw JsonParseException("Cannot parse LocalDateTime from $json")
    }
}

class InstantAdapter : JsonSerializer<Instant>, JsonDeserializer<Instant> {
    override fun serialize(src: Instant?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return if (src == null) JsonNull.INSTANCE else JsonPrimitive(src.toString())
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Instant {
        if (json == null || json.isJsonNull) throw JsonParseException("Null Instant")
        if (json.isJsonPrimitive) {
            return Instant.parse(json.asString)
        }
        if (json.isJsonObject) {
            val obj = json.asJsonObject
            if (obj.size() == 0) return Instant.EPOCH // Fallback
            val seconds = obj.get("seconds")?.asLong
            val nanos = obj.get("nanos")?.asInt
            if (seconds != null && nanos != null) {
                return Instant.ofEpochSecond(seconds, nanos.toLong())
            }
        }
        throw JsonParseException("Cannot parse Instant from $json")
    }
}

class YearMonthAdapter : JsonSerializer<YearMonth>, JsonDeserializer<YearMonth> {
    override fun serialize(src: YearMonth?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return if (src == null) JsonNull.INSTANCE else JsonPrimitive(src.toString())
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): YearMonth {
        if (json == null || json.isJsonNull) throw JsonParseException("Null YearMonth")
        if (json.isJsonPrimitive) {
            return YearMonth.parse(json.asString)
        }
        if (json.isJsonObject) {
            val obj = json.asJsonObject
            if (obj.size() == 0) return YearMonth.of(1970, 1) // Fallback
            val year = obj.get("year")?.asInt
            val month = obj.get("month")?.asInt
            if (year != null && month != null) {
                return YearMonth.of(year, month)
            }
        }
        throw JsonParseException("Cannot parse YearMonth from $json")
    }
}
