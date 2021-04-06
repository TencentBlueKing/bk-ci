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

package com.tencent.bkrepo.helm.utils

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object TimeFormatUtil {

    private val DATA_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

    fun getUtcTime(): String {
        return Instant.now().toString()
    }

    fun formatLocalTime(localDateTime: LocalDateTime): String {
        return localDateTime.format(DateTimeFormatter.ISO_DATE_TIME)
    }

    fun convertToLocalTime(utcTime: String): LocalDateTime {
        return convertToLocalTime(LocalDateTime.parse(utcTime, DateTimeFormatter.ISO_DATE_TIME))
    }

    private fun convertToLocalTime(utcTime: LocalDateTime): LocalDateTime {
        return utcTime.plusHours(8)
    }

    fun convertToUtcTime(localDateTime: LocalDateTime): String {
        return toUtc(localDateTime).format(DATA_TIME_FORMATTER)
    }

    private fun toUtc(time: LocalDateTime): LocalDateTime {
        return toUtc(time, ZoneId.systemDefault())
    }

    private fun toUtc(time: LocalDateTime, fromZone: ZoneId): LocalDateTime {
        return toZone(time, fromZone, ZoneOffset.UTC)
    }

    private fun toZone(time: LocalDateTime, fromZone: ZoneId, toZone: ZoneId): LocalDateTime {
        val zonedTime = time.atZone(fromZone)
        val converted = zonedTime.withZoneSameInstant(toZone)
        return converted.toLocalDateTime()
    }
}
