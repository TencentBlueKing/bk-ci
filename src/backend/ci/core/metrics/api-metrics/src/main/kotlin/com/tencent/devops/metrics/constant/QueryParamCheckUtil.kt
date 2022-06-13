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

package com.tencent.devops.metrics.constant

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.DateTimeUtil
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
    ) = ChronoUnit.DAYS.between(fromDate, toDate)

    val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    fun getStartDateTime(): String {
        val startDateTime = LocalDate.now().minusDays(1).minusMonths(1)
        return startDateTime.format(DATE_FORMATTER)
    }
    fun getEndDateTime(): String {
        val endDateTime = LocalDate.now()
        return endDateTime.format(DATE_FORMATTER)
    }

    fun toMinutes(millisecond: Long): Double {
        val min = TimeUnit.MILLISECONDS.toMinutes(millisecond)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millisecond - min * 60000).toDouble()
        return String.format("%.2f", min.toDouble() + seconds / 60).toDouble()
    }

    fun getBetweenDate(start: String, end: String): List<String> {
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

    fun checkParam(userId: String, projectId: String, startTime: String, endTime: String) {
        if (userId.isBlank()) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                params = arrayOf("userId")
            )
        }
        if (projectId.isBlank()) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                params = arrayOf("projectId")
            )
        }
//        val startDate = DateTimeUtil.stringToLocalDate(startTime)
//        val endDate = DateTimeUtil.stringToLocalDate(endTime)
//        val firstDate = LocalDate.now().minusDays(1)
//        val secondDate = firstDate.minusMonths(6)
//        if (startDate!!.isBefore(secondDate)) {
//            throw ErrorCodeException(
//                errorCode = MetricsMessageCode.QUERY_DATE_BEYOND,
//            )
//        }
//        if (endDate!!.isAfter(firstDate)) {
//            throw ErrorCodeException(
//                errorCode = MetricsMessageCode.QUERY_DATE_BEYOND,
//            )
//        }
    }
}