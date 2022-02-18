package com.tencent.devops.common.util

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule

object JsonUtil {
    private val objectMapper = ObjectMapper().apply {
        registerModule(KotlinModule())
        configure(SerializationFeature.INDENT_OUTPUT, true)
        configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
        configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true)
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
    }

    private val skipEmptyObjectMapper = ObjectMapper().apply {
        registerModule(KotlinModule())
        configure(SerializationFeature.INDENT_OUTPUT, true)
        configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
        configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true)
        setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
    }

    private fun getObjectMapper() = objectMapper

    /**
     * 转成Json
     */
    fun toJson(bean: Any?): String {
        if (bean == null) {
            return "null"
        }
        if (ReflectUtil.isNativeType(bean) || bean is String) {
            return bean.toString()
        }
        return getObjectMapper().writeValueAsString(bean)!!
    }

    /**
     * 将对象转可修改的Map,
     * 注意：会忽略掉值为空串和null的属性
     */
    fun toMutableMapSkipEmpty(bean: Any): MutableMap<String, Any> {
        if (ReflectUtil.isNativeType(bean)) {
            return mutableMapOf()
        }
        return if (bean is String)
            skipEmptyObjectMapper.readValue<MutableMap<String, Any>>(
                bean.toString(),
                object : TypeReference<MutableMap<String, Any>>() {}
            )
        else
            skipEmptyObjectMapper.readValue<MutableMap<String, Any>>(
                skipEmptyObjectMapper.writeValueAsString(bean),
                object : TypeReference<MutableMap<String, Any>>() {}
            )
    }

    /**
     * 将对象转不可修改的Map
     * 注意：会忽略掉值为null的属性
     */
    fun toMap(bean: Any): Map<String, Any> {
        return when {
            ReflectUtil.isNativeType(bean) -> mapOf()
            bean is String -> to(bean)
            else -> to(getObjectMapper().writeValueAsString(bean))
        }
    }

    /**
     * 将json转指定类型对象
     * @param json json字符串
     * @return 指定对象
     */
    fun <T> to(json: String): T {
        return getObjectMapper().readValue<T>(json, object : TypeReference<T>() {})
    }

    fun <T> to(json: String, typeReference: TypeReference<T>): T {
        return getObjectMapper().readValue<T>(json, typeReference)
    }

    fun <T> to(json: String, type: Class<T>): T = getObjectMapper().readValue(json, type)

    fun <T> toOrNull(json: String?, type: Class<T>): T? {
        return if (json.isNullOrBlank()) {
            null
        } else {
            getObjectMapper().readValue(json, type)
        }
    }

    fun <T> mapTo(map: Map<String, Any>, type: Class<T>): T = getObjectMapper().readValue(
        getObjectMapper().writeValueAsString(map), type
    )
}
