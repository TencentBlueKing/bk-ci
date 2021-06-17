package com.tencent.bk.codecc.apiquery.utils

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer

class EntityIdDeserializer : JsonDeserializer<String>() {
    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): String {
        return try {
            val map = jp.readValueAs(Map::class.java)
            if (!map.isNullOrEmpty()) {
                map["\$oid"] as String
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }
}