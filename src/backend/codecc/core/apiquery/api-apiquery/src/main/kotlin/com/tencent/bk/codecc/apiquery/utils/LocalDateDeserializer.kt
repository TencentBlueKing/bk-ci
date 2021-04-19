package com.tencent.bk.codecc.apiquery.utils

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class LocalDateDeserializer : JsonDeserializer<LocalDate>() {
    @Throws(IOException::class)
    override fun deserialize(p: JsonParser, deserializationContext: DeserializationContext): LocalDate? {
        return try {
            val map = p.readValueAs(Map::class.java)
            if (!map.isNullOrEmpty()) {
                LocalDate.parse(map["\$date"].toString(), DateTimeFormatter.ISO_DATE_TIME).plusDays(1)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}