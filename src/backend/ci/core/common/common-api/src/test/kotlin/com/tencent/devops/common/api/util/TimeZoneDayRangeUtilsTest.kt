/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent. All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 */

package com.tencent.devops.common.api.util

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneId

class TimeZoneDayRangeUtilsTest {

    @Test
    fun ofCalendarDatesUtc() {
        val range = TimeZoneDayRangeUtils.ofCalendarDates(
            startDate = "2020-07-06",
            endDate = "2020-07-06",
            timeZone = "UTC"
        )
        Assertions.assertEquals(
            LocalDate.of(2020, 7, 6).atStartOfDay(ZoneId.of("UTC")).toInstant(),
            range.startInclusive
        )
        Assertions.assertEquals(
            LocalDate.of(2020, 7, 7).atStartOfDay(ZoneId.of("UTC")).toInstant(),
            range.endExclusive
        )
        val (startMs, endMs) = range.toEpochMilli()
        Assertions.assertEquals(range.startInclusive.toEpochMilli(), startMs)
        Assertions.assertEquals(range.endExclusive.toEpochMilli(), endMs)
    }

    @Test
    fun startOfDayEpochMilliShanghai() {
        val ms = TimeZoneDayRangeUtils.startOfDayEpochMilli("2020-07-06", "Asia/Shanghai")
        val expected = LocalDate.of(2020, 7, 6)
            .atStartOfDay(ZoneId.of("Asia/Shanghai"))
            .toInstant()
            .toEpochMilli()
        Assertions.assertEquals(expected, ms)
    }
}
