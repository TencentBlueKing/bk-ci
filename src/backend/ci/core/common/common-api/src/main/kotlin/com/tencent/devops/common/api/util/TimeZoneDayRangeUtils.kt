/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent. All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 */

package com.tencent.devops.common.api.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 按用户时区将日历日换算为绝对时刻区间，供按日/按区间查询使用。
 *
 * 契约见 docs/specification/timezone_and_datetime.md
 */
object TimeZoneDayRangeUtils {

    data class DayRange(
        val startInclusive: Instant,
        val endExclusive: Instant
    ) {
        fun toEpochMilli(): Pair<Long, Long> = startInclusive.toEpochMilli() to endExclusive.toEpochMilli()

        /**
         * 转为 DB/应用层常用的 LocalDateTime（按 [ZoneId.systemDefault] 解释）。
         */
        fun toSystemLocalDateTime(): Pair<LocalDateTime, LocalDateTime> {
            val systemZone = ZoneId.systemDefault()
            return LocalDateTime.ofInstant(startInclusive, systemZone) to
                LocalDateTime.ofInstant(endExclusive, systemZone)
        }
    }

    /**
     * @param startDate 起始日 yyyy-MM-dd（含）
     * @param endDate 结束日 yyyy-MM-dd（含该日全天，返回区间为次日 00:00 开区间）
     * @param timeZone IANA 时区，空则使用 systemDefault
     */
    fun ofCalendarDates(
        startDate: String,
        endDate: String,
        timeZone: String? = null
    ): DayRange {
        val zone = DateTimeUtil.resolveZoneId(timeZone)
        val start = LocalDate.parse(startDate).atStartOfDay(zone).toInstant()
        val endExclusive = LocalDate.parse(endDate).plusDays(1).atStartOfDay(zone).toInstant()
        return DayRange(start, endExclusive)
    }

    fun startOfDayEpochMilli(date: String, timeZone: String? = null): Long {
        val zone = DateTimeUtil.resolveZoneId(timeZone)
        return LocalDate.parse(date).atStartOfDay(zone).toInstant().toEpochMilli()
    }

    fun endOfDayExclusiveEpochMilli(date: String, timeZone: String? = null): Long {
        val zone = DateTimeUtil.resolveZoneId(timeZone)
        return LocalDate.parse(date).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    }
}
