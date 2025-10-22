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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter
import java.util.Date

class DateTimeUtilTest {

    @Test
    fun minuteToSecondTest() {
        val minutes = 60
        val expected = 3600
        Assertions.assertEquals(expected, DateTimeUtil.minuteToSecond(minutes))
    }

    @Test
    fun secondToMinuteTest() {
        val minutes = 3600
        val expected = 60
        Assertions.assertEquals(expected, DateTimeUtil.secondToMinute(minutes))
    }

    @Test
    fun formatDateTest() {
        val date = Date.from(LocalDateTime.of(2020, 7, 6, 1, 59, 59).toInstant(UTC))
        val format = "yyyy-MM-dd HH:mm:ss"
        val simpleDateFormat = SimpleDateFormat(format)
        Assertions.assertEquals(simpleDateFormat.format(date), DateTimeUtil.formatDate(date, format))
    }

    @Test
    fun convertLocalDateTimeToTimestampTest() {
        val date = LocalDateTime.of(2020, 7, 6, 1, 59, 59)
        val expected: Long = 1593971999
        Assertions.assertEquals(expected, DateTimeUtil.convertLocalDateTimeToTimestamp(date))
    }

    @Test
    fun toDateTimeTest() {
        val date = LocalDateTime.of(2020, 7, 6, 1, 59, 59)
        val format = "yyyy-MM-dd HH:mm:ss"
        val expected = "2020-07-06 01:59:59"
        Assertions.assertEquals(expected, DateTimeUtil.toDateTime(date, format))
    }

    @Test
    fun zoneDateToTimestampTest() {
        val date = "2019-09-02T08:58:46+0000"
        val expected: Long = 1567414726000
        Assertions.assertEquals(expected, DateTimeUtil.zoneDateToTimestamp(date))
        val date2 = "2019-09-02T08:58:46Z"
        Assertions.assertEquals(expected, DateTimeUtil.zoneDateToTimestamp(date2))
    }

    @Test
    fun formatTimeTest() {
        val date = "1567414726000"
        val expected = "435392979时26分40秒"
        Assertions.assertEquals(expected, DateTimeUtil.formatTime(date))
    }

    @Test
    fun formatMilliTimeTest() {
        val date = "1567414726000"
        val expected = "435392时58分46秒"
        Assertions.assertEquals(expected, DateTimeUtil.formatMilliTime(date))
    }

    @Test
    fun formatMilliTimeStringTest() {
        val date: Long = 1567414726000
        val expected = "435392时58分46秒"
        Assertions.assertEquals(expected, DateTimeUtil.formatMilliTime(date))
    }

    @Test
    fun formatMillSecondTest() {
        val date: Long = 1567414726000
        val expected = "18141天8时58分46秒"
        Assertions.assertEquals(expected, DateTimeUtil.formatMillSecond(date))
    }

    @Test
    fun stringToLocalDateTimeTest() {
        val date = "2020-07-06 01:59:59"
        val format = "yyyy-MM-dd HH:mm:ss"
        val expected = LocalDateTime.parse("2020-07-06 01:59:59", DateTimeFormatter.ofPattern(format))
        Assertions.assertEquals(expected, DateTimeUtil.stringToLocalDateTime(date, format))
    }

    @Test
    fun test() {
        val date = LocalDateTime.of(2020, 12, 15, 21, 9, 59)
        val dateStr = DateTimeUtil.toDateTime(date)
        println(dateStr)
    }

    @Test
    fun outOfBoundsInt() {
        val year = 525600
        val lastTime = (DateTimeUtil.minuteToSecond(year) * 1000)
        val maxInt = Int.MAX_VALUE
        Assertions.assertFalse(lastTime - maxInt > 0)
    }

    @Test
    fun outOfBoundsLong() {
        val year = 525600
        val lastTime = (DateTimeUtil.minuteToSecond(year).toLong() * 1000)
        val maxInt = Int.MAX_VALUE
        Assertions.assertTrue(lastTime - maxInt > 0)
    }

    @Test
    fun stringToLocalDateTime() {
        var dateStr = "2021"
        var convertDate = DateTimeUtil.stringToLocalDateTime(dateStr, "yyyy")
        Assertions.assertEquals(convertDate.toString(), "2021-01-01T00:00")
        dateStr = "2021-04"
        convertDate = DateTimeUtil.stringToLocalDateTime(dateStr, "yyyy-MM")
        Assertions.assertEquals(convertDate.toString(), "2021-04-01T00:00")
        dateStr = "2021-04-29"
        convertDate = DateTimeUtil.stringToLocalDateTime(dateStr, DateTimeUtil.YYYY_MM_DD)
        Assertions.assertEquals(convertDate.toString(), "2021-04-29T00:00")
        dateStr = "2021-04-29 15"
        convertDate = DateTimeUtil.stringToLocalDateTime(dateStr, "yyyy-MM-dd HH")
        Assertions.assertEquals(convertDate.toString(), "2021-04-29T15:00")
        dateStr = "2021-04-29 15:02"
        convertDate = DateTimeUtil.stringToLocalDateTime(dateStr, "yyyy-MM-dd HH:mm")
        Assertions.assertEquals(convertDate.toString(), "2021-04-29T15:02")
        dateStr = "2021-04-29 15:02:01"
        convertDate = DateTimeUtil.stringToLocalDateTime(dateStr, "yyyy-MM-dd HH:mm:ss")
        Assertions.assertEquals(convertDate.toString(), "2021-04-29T15:02:01")
    }
}
