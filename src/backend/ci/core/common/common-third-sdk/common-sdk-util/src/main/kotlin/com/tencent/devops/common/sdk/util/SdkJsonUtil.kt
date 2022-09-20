package com.tencent.devops.common.sdk.util

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.tencent.devops.common.sdk.json.JsonIgnorePathAnnotationIntrospector
import java.lang.reflect.Type

object SdkJsonUtil {
    private val OBJECT_MAPPER = ObjectMapper().apply {
        registerModule(KotlinModule())
        enable(SerializationFeature.INDENT_OUTPUT)
        enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
        enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature())
        setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        setAnnotationIntrospectors(
            AnnotationIntrospectorPair.pair(
                serializationConfig.annotationIntrospector, JsonIgnorePathAnnotationIntrospector()
            ),
            AnnotationIntrospectorPair.pair(
                deserializationConfig.annotationIntrospector, JsonIgnorePathAnnotationIntrospector()
            )
        )
    }

    fun getObjectMapper(): ObjectMapper {
        return OBJECT_MAPPER
    }

    fun <T> fromJson(jsonStr: String, clazz: Class<T>): T {
        return OBJECT_MAPPER.readValue(jsonStr, clazz)
    }

    fun <T> fromJson(jsonStr: String, typeReference: TypeReference<T>): T {
        return OBJECT_MAPPER.readValue(jsonStr, typeReference)
    }

    fun <T> fromJson(jsonStr: String, type: Type): T {
        val javaType = OBJECT_MAPPER.typeFactory.constructType(type)
        return OBJECT_MAPPER.readValue(jsonStr, javaType)
    }

    fun toJson(obj: Any): String {
        return OBJECT_MAPPER.writeValueAsString(obj)
    }
}
