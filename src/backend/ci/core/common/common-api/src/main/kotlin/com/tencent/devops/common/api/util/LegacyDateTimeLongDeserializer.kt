/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoField

/**
 * 时区改造后接口时间字段改为毫秒时间戳。滚动发布期间旧服务仍返回
 * "yyyy-MM-dd HH:mm:ss"，这里让 Long 同时接受日期字符串和数字。
 */
class LegacyDateTimeLongDeserializer(
    private val delegate: JsonDeserializer<Long>
) : JsonDeserializer<Long?>() {

    /**
     * 返回值必须可空。Jackson 会把字符串 "null" 和 JSON null 解成 null，
     * 若这里声明成非空 Long，Kotlin 会在返回时抛出空指针。
     */
    override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): Long? {
        if (parser.currentToken == JsonToken.VALUE_STRING) {
            parseLegacyDateTime(parser.text)?.let { return it }
        }
        return delegate.deserialize(parser, ctxt)
    }

    override fun getNullValue(ctxt: DeserializationContext): Long? = delegate.getNullValue(ctxt)

    companion object {
        private val DATE_TIME: DateTimeFormatter = DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .optionalStart()
            .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true)
            .optionalEnd()
            .toFormatter()

        private val LEGACY_PATTERN =
            Regex("""\d{4}-\d{2}-\d{2}([ T]\d{2}:\d{2}:\d{2}(\.\d{1,9})?)?""")

        fun parseLegacyDateTime(text: String?): Long? {
            val value = text?.trim().orEmpty()
            if (!LEGACY_PATTERN.matches(value)) {
                return null
            }
            val dateTime = try {
                if (value.length == DATE_ONLY_LENGTH) {
                    LocalDate.parse(value).atStartOfDay()
                } else {
                    LocalDateTime.parse(value.replace('T', ' '), DATE_TIME)
                }
            } catch (ignored: DateTimeParseException) {
                return null
            }
            return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        private const val DATE_ONLY_LENGTH = 10
    }
}
