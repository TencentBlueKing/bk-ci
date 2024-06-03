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

package com.tencent.devops.metrics.utils

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.metrics.config.MetricsConfig
import com.tencent.devops.metrics.constant.Constants.ERROR_TYPE_NAME_PREFIX
import com.tencent.devops.metrics.constant.MetricsMessageCode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import java.util.stream.Stream

object QueryParamCheckUtil {

    fun getIntervalTime(
        fromDate: LocalDateTime,
        toDate: LocalDateTime
    ) = if (fromDate.isEqual(toDate)) 1 else ChronoUnit.DAYS.between(fromDate, toDate)

    val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    fun getStartDateTime(): String {
        val startDateTime = LocalDate.now().minusMonths(1)
        return startDateTime.format(DATE_FORMATTER)
    }
    fun getEndDateTime(): String {
        val endDateTime = LocalDate.now()
        return endDateTime.format(DATE_FORMATTER)
    }

    fun toMinutes(millisecond: Long): Double {
        if (millisecond == 0L) return 0.0
        val min = TimeUnit.MILLISECONDS.toMinutes(millisecond)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millisecond - min * 60000).toDouble()
        return String.format("%.2f", min.toDouble() + seconds / 60).toDouble()
    }

    fun getBetweenDate(start: String, end: String): List<String> {
        if (start == end) {
            return listOf(start)
        }
        val list: MutableList<String> = ArrayList()
        // LocalDate默认的时间格式为2020-02-02
        val startDate: LocalDate = LocalDate.parse(start)
        val endDate: LocalDate = LocalDate.parse(end)
        val distance = ChronoUnit.DAYS.between(startDate, endDate)
        if (distance < 1) {
            return list
        }
        Stream.iterate(startDate) { d -> d.plusDays(1) }
            .limit(distance + 1).forEach { f -> list.add(f.toString()) }
        return list
    }

    fun checkDateInterval(startTime: String, endTime: String) {
        val metricsConfig = MetricsConfig()
        // 目前仅支持6个月内的数据查询
        val startDate = DateTimeUtil.stringToLocalDate(startTime)
        val endDate = DateTimeUtil.stringToLocalDate(endTime)
        if ((startDate!!.until(endDate, ChronoUnit.DAYS)) > metricsConfig.queryDaysMax) {
            throw ErrorCodeException(
                errorCode = MetricsMessageCode.QUERY_DATE_BEYOND,
                params = arrayOf("${metricsConfig.queryDaysMax}")
            )
        }
        val currentDate = LocalDate.now()
        if (startDate.isBefore(currentDate) && ((startDate.until(
                currentDate,
                ChronoUnit.DAYS
            )) > metricsConfig.queryDaysMax)
        ) {
            throw ErrorCodeException(
                errorCode = MetricsMessageCode.QUERY_DATE_BEYOND,
                params = arrayOf("${metricsConfig.queryDaysMax}")
            )
        }
    }

    fun getErrorTypeName(errorType: Int): String {
        return I18nUtil.getCodeLanMessage(ERROR_TYPE_NAME_PREFIX + "$errorType")
    }
}
