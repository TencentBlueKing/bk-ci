/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.api.util

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule

/**
 *
 * Powered By Tencent
 */
object JsonUtil {
    private val objectMapper = ObjectMapper().apply {
        registerModule(KotlinModule())
        configure(SerializationFeature.INDENT_OUTPUT, true)
        configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
        configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true)
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
    }

    private val skipEmptyObjectMapper = ObjectMapper().apply {
        registerModule(KotlinModule())
        configure(SerializationFeature.INDENT_OUTPUT, true)
        configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
        configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true)
        setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
    }

    fun getObjectMapper() = objectMapper

    fun registerModule(vararg subModules: Module) {
        subModules.forEach { subModule ->
            objectMapper.registerModule(subModule)
            skipEmptyObjectMapper.registerModule(subModule)
        }
    }

    /**
     * 转成Json
     */
    fun toJson(bean: Any): String {
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
                object : TypeReference<MutableMap<String, Any>>() {})
        else
            skipEmptyObjectMapper.readValue<MutableMap<String, Any>>(
                skipEmptyObjectMapper.writeValueAsString(bean),
                object : TypeReference<MutableMap<String, Any>>() {})
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
     * 这个只能做简单的转换List<String>, Map类型的，如果是自定义的类会被kotlin擦除成hashMap
     * @param json json字符串
     * @return 指定对象
     */
    private fun <T> to(json: String): T {
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
