package com.tencent.bk.codecc.apiquery.utils

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class LocalDateTimeDeserializer : JsonDeserializer<LocalDateTime>() {
    @Throws(IOException::class)
    override fun deserialize(p: JsonParser, deserializationContext: DeserializationContext): LocalDateTime? {
        return try {
            val map = p.readValueAs(Map::class.java)
            if (!map.isNullOrEmpty()) {
                LocalDateTime.parse(map["\$date"].toString(), DateTimeFormatter.ISO_DATE_TIME).plusHours(8)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}