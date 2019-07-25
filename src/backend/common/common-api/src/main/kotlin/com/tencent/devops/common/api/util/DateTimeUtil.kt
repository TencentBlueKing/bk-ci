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

import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Calendar
import java.util.Date

/**
 *
 * Powered By Tencent
 */
fun LocalDateTime.timestamp(): Long {
    val zoneId = ZoneId.systemDefault()
    return this.atZone(zoneId).toInstant().epochSecond
}

fun LocalDateTime.timestampmilli(): Long {
    val zoneId = ZoneId.systemDefault()
    return this.atZone(zoneId).toInstant().toEpochMilli()
}

object DateTimeUtil {
    /**
     * 获取从当前开始一定单位时间间隔的日期
     * @param unit 单位 Calendar.SECONDS
     * @param timeSpan 实际间隔
     * @return 日期类实例
     */
    fun getFutureDateFromNow(unit: Int, timeSpan: Int): Date {
        val cd = Calendar.getInstance()
        cd.time = Date()
        cd.add(unit, timeSpan)
        return cd.time
    }

    /**
     * 按指定日期时间格式格式化日期时间
     * @param date 日期时间
     * @param format 格式化字符串
     * @return 字符串
     */
    fun formatDate(date: Date, format: String = "yyyy-MM-dd HH:mm:ss"): String {
        val simpleDateFormat = SimpleDateFormat(format)
        return simpleDateFormat.format(date)
    }

    fun convertLocalDateTimeToTimestamp(localDateTime: LocalDateTime?): Long {
        return localDateTime?.toEpochSecond(ZoneOffset.ofHours(8)) ?: 0L
    }

    fun toDateTime(dateTime: LocalDateTime?, format: String = "yyyy-MM-dd HH:mm:ss"): String {
        if (dateTime == null) {
            return ""
        }
        val zone = ZoneId.systemDefault()
        val instant = dateTime.atZone(zone).toInstant()
        val simpleDateFormat = SimpleDateFormat(format)
        return simpleDateFormat.format(Date.from(instant))
    }

    /**
     * 毫秒时间
     * Long类型时间转换成视频时长
     */
    fun formatTime(timeStr: String): String {
        val timeGap = 60
        val million = 1000
        val zero = 0L
        val ten = 10
        val time = timeStr.toLong() * million
        val hour = time / (timeGap * timeGap * million)
        val minute = (time - hour * timeGap * timeGap * million) / (timeGap * million)
        val second = (time - hour * timeGap * timeGap * million - minute * timeGap * million) / million
        return (if (hour == zero) "00" else if (hour >= ten) hour.toString() else "0$hour").toString() + "时" +
            (if (minute == zero) "00" else if (minute >= ten) minute else "0$minute") + "分" +
            (if (second == zero) "00" else if (second >= ten) second.toShort() else "0$second") + "秒"
    }
}