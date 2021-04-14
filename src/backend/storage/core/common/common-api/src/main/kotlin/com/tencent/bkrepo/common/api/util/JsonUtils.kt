/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.common.api.util

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import java.io.InputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter.ISO_DATE
import java.time.format.DateTimeFormatter.ISO_DATE_TIME
import java.time.format.DateTimeFormatter.ISO_TIME

/**
 * Json工具类
 */
object JsonUtils {
    val objectMapper = jacksonObjectMapper().apply {
        val javaTimeModule = JavaTimeModule()

        javaTimeModule.addSerializer(LocalTime::class.java, LocalTimeSerializer(ISO_TIME))
        javaTimeModule.addSerializer(LocalDate::class.java, LocalDateSerializer(ISO_DATE))
        javaTimeModule.addSerializer(LocalDateTime::class.java, LocalDateTimeSerializer(ISO_DATE_TIME))
        javaTimeModule.addDeserializer(LocalTime::class.java, LocalTimeDeserializer(ISO_TIME))
        javaTimeModule.addDeserializer(LocalDate::class.java, LocalDateDeserializer(ISO_DATE))
        javaTimeModule.addDeserializer(LocalDateTime::class.java, LocalDateTimeDeserializer(ISO_DATE_TIME))
        registerModule(javaTimeModule)
        registerModule(ParameterNamesModule())
        registerModule(Jdk8Module())

        enable(SerializationFeature.INDENT_OUTPUT)
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
    }
}

/**
 * 将对象序列化为json字符串
 */
fun Any.toJsonString() = JsonUtils.objectMapper.writeValueAsString(this).orEmpty()

/**
 * 将json字符串反序列化为对象
 */
inline fun <reified T> String.readJsonString(): T = JsonUtils.objectMapper.readValue(this, jacksonTypeRef<T>())

/**
 * 将json字符串流反序列化为对象
 */
inline fun <reified T> InputStream.readJsonString(): T = JsonUtils.objectMapper.readValue(this, jacksonTypeRef<T>())
