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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.api.util

import com.fasterxml.jackson.annotation.JsonFilter
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.ser.FilterProvider
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.api.annotation.SkipLogField
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter.ISO_DATE
import java.time.format.DateTimeFormatter.ISO_DATE_TIME
import java.time.format.DateTimeFormatter.ISO_TIME

/**
 *
 * Powered By Tencent
 */
@Suppress("TooManyFunctions")
object JsonUtil {

    private const val MAX_CLAZZ = 50000L

    /**
     * 序列化时忽略bean中的某些字段,字段需要用注解SkipLogFields声明
     *
     * @param bean 对象
     * @param <T>  对象类型
     * @return Json字符串
     * @see SkipLogField
    </T> */
    fun <T : Any> skipLogFields(bean: T): String? {
        return try {
            beanMapperCache.get(bean.javaClass)!!.writeValueAsString(bean)
        } catch (ignored: Throwable) {
            loadMapper(bean.javaClass).writeValueAsString(bean)
        }
    }

    // 如果出现50000+以上的不同的数据类（不是对象）时。。。
    // 系统性能一定会下降，永久代区可能会OOM了，但不会是在这里引起的。所以这里限制了一个几乎不可能达到的值
    private val beanMapperCache = Caffeine.newBuilder().maximumSize(MAX_CLAZZ)
        .build<Class<Any>, ObjectMapper> { clazz -> loadMapper(clazz) }

    private fun loadMapper(clazz: Class<Any>): ObjectMapper {
        val nonEmptyMapper = objectMapper()
        var aClass: Class<*>? = clazz // bean.javaClass
        val skipFields: MutableSet<String> = HashSet()
        val skipLogFieldClass = SkipLogField::class.java
        while (aClass != null) {
            val fields = aClass.declaredFields
            for (field in fields) {
                val fieldAnnotation = field.getAnnotation(skipLogFieldClass) ?: continue
                if (fieldAnnotation.value.trim().isNotEmpty()) {
                    skipFields.add(fieldAnnotation.value)
                } else {
                    skipFields.add(field.name)
                }
            }
            aClass = aClass.superclass
        }
        if (skipFields.isNotEmpty()) {
            nonEmptyMapper.addMixIn(clazz, skipLogFieldClass)
            // 仅包含
            val filterProvider: FilterProvider = SimpleFilterProvider()
                .addFilter(
                    skipLogFieldClass.getAnnotation(JsonFilter::class.java).value,
                    SimpleBeanPropertyFilter.serializeAllExcept(skipFields)
                )
            nonEmptyMapper.setFilterProvider(filterProvider)
        }
        return nonEmptyMapper
    }

    private val jsonModules = mutableSetOf<Module>()

    private val objectMapper = objectMapper()

    private fun objectMapper(): ObjectMapper {
        return ObjectMapper().apply {
            registerModule(javaTimeModule())
            registerModule(KotlinModule())
            enable(SerializationFeature.INDENT_OUTPUT)
            enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
            enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature())
            setSerializationInclusion(JsonInclude.Include.NON_NULL)
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            jsonModules.forEach { jsonModule ->
                registerModule(jsonModule)
            }
        }
    }

    private val skipEmptyObjectMapper = ObjectMapper().apply {
        registerModule(javaTimeModule())
        registerModule(KotlinModule())
        enable(SerializationFeature.INDENT_OUTPUT)
        enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
        enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature())
        setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        jsonModules.forEach { jsonModule ->
            registerModule(jsonModule)
        }
    }

    private fun javaTimeModule(): JavaTimeModule {
        val javaTimeModule = JavaTimeModule()

        javaTimeModule.addSerializer(LocalTime::class.java, LocalTimeSerializer(ISO_TIME))
        javaTimeModule.addSerializer(LocalDate::class.java, LocalDateSerializer(ISO_DATE))
        javaTimeModule.addSerializer(LocalDateTime::class.java, LocalDateTimeSerializer(ISO_DATE_TIME))
        javaTimeModule.addDeserializer(LocalTime::class.java, LocalTimeDeserializer(ISO_TIME))
        javaTimeModule.addDeserializer(LocalDate::class.java, LocalDateDeserializer(ISO_DATE))
        javaTimeModule.addDeserializer(LocalDateTime::class.java, LocalDateTimeDeserializer(ISO_DATE_TIME))
        return javaTimeModule
    }

    private val unformattedObjectMapper = objectMapper().apply { disable(SerializationFeature.INDENT_OUTPUT) }

    fun getObjectMapper(formatted: Boolean = true) = if (formatted) objectMapper else unformattedObjectMapper

    /**
     * 此方法仅在系统初始化时调用，不建议在运行过程中调用
     * [subModules]子模块/类注册最佳时机是在系统初始化时调用，而不是在运行过程中
     */
    fun registerModule(vararg subModules: Module) {
        synchronized(jsonModules) {
            // 过量保护，子类过多会导致解析慢，系统业务正常情况下永远不应该达到该值，如果出现必须出错
            if (jsonModules.size < MAX_CLAZZ) {
                jsonModules.addAll(subModules)
            }
        }
        subModules.forEach { subModule ->
            objectMapper.registerModule(subModule)
            skipEmptyObjectMapper.registerModule(subModule)
            unformattedObjectMapper.registerModule(subModule)
        }
    }

    /**
     * 转成Json, [formatted]默认ture采用格式化方式输出
     */
    fun toJson(bean: Any, formatted: Boolean = true): String {
        if (ReflectUtil.isNativeType(bean) || bean is String) {
            return bean.toString()
        }
        return getObjectMapper(formatted).writeValueAsString(bean)!!
    }

    /**
     * 将对象转可修改的Map,
     * 注意：会忽略掉值为空串和null的属性
     */
    @Deprecated("不建议使用，建议使用toMutableMap")
    fun toMutableMapSkipEmpty(bean: Any): MutableMap<String, Any> {
        if (ReflectUtil.isNativeType(bean)) {
            return mutableMapOf()
        }
        return if (bean is String) {
            skipEmptyObjectMapper.readValue(bean.toString(), object : TypeReference<MutableMap<String, Any>>() {})
        } else {
            skipEmptyObjectMapper.readValue(
                skipEmptyObjectMapper.writeValueAsString(bean),
                object : TypeReference<MutableMap<String, Any>>() {}
            )
        }
    }

    /**
     * 将对象转不可修改的Map
     * 注意：会忽略掉值为null的属性
     */
    fun toMap(bean: Any): Map<String, Any> {
        return toMutableMap(bean)
    }

    /**
     * 将对象转不可修改的Map
     * 注意：会忽略掉值为null的属性, 不会忽略空串和空数组/列表对象
     */
    fun toMutableMap(bean: Any): MutableMap<String, Any> {
        return when {
            ReflectUtil.isNativeType(bean) -> mutableMapOf()
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
    fun <T> to(json: String): T {
        return getObjectMapper().readValue<T>(json, object : TypeReference<T>() {})
    }

    fun <T> to(json: String, typeReference: TypeReference<T>): T {
        return getObjectMapper().readValue<T>(json, typeReference)
    }

    fun <T> to(json: String, type: Class<T>): T = getObjectMapper().readValue(json, type)

    fun <T> toOrNull(json: String?, type: Class<T>): T? {
        return json?.let { self ->
            if (self.isBlank()) {
                return null
            }
            try {
                getObjectMapper().readValue(self, type)
            } catch (ignore: Exception) {
                null
            }
        }
    }

    fun <T> toOrNull(json: String?, typeReference: TypeReference<T>): T? {
        return json?.let { self ->
            if (self.isBlank()) {
                return null
            }
            try {
                getObjectMapper().readValue(self, typeReference)
            } catch (ignore: Exception) {
                null
            }
        }
    }

    fun <T> mapTo(map: Map<String, Any>, type: Class<T>): T = getObjectMapper().readValue(
        getObjectMapper().writeValueAsString(map), type
    )

    fun <T> anyTo(any: Any?, typeReference: TypeReference<T>): T = getObjectMapper().readValue(
        getObjectMapper().writeValueAsString(any), typeReference
    )

    @Suppress("UNCHECKED_CAST")
    fun <T> Any.deepCopy(): T {
        return getObjectMapper().readValue(getObjectMapper().writeValueAsString(this), this.javaClass) as T
    }
}
