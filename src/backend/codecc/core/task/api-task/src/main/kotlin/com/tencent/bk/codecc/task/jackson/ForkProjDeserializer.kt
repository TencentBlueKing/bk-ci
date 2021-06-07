package com.tencent.bk.codecc.task.jackson

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.tencent.bk.codecc.task.pojo.ForkProjModel
import lombok.extern.slf4j.Slf4j
import org.slf4j.LoggerFactory

@Slf4j
class ForkProjDeserializer : JsonDeserializer<ForkProjModel>() {

    companion object {
        private val logger = LoggerFactory.getLogger(ForkProjDeserializer::class.java)
    }

    override fun deserialize(jp: JsonParser, context: DeserializationContext): ForkProjModel? {
        return try {
            jp.readValueAs(ForkProjModel::class.java)
        } catch (e: Exception) {
            null
        }
    }
}