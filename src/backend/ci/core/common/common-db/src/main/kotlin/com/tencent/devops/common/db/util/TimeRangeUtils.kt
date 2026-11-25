/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 */

package com.tencent.devops.common.db.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object TimeRangeUtils {

    fun getTodayRange(timeZone: String? = null): Pair<LocalDateTime, LocalDateTime> {
        val zone = resolveZoneId(timeZone)
        val today = LocalDate.now(zone)
        return getCalendarDateRange(today, today, zone)
    }

    fun getTimeRange(date: LocalDateTime): Pair<LocalDateTime, LocalDateTime> {
        val start = LocalDateTime.of(date.year, date.month, date.dayOfMonth, 0, 0, 0)
        val end = start.plusDays(1)
        return Pair(start, end)
    }

    /**
     * 按指定时区的自然日区间，转为 DB 使用的 LocalDateTime 区间（systemDefault）。
     * 返回 Pair(startInclusive, endExclusive)。
     */
    fun getCalendarDateRange(
        startDate: LocalDate,
        endDate: LocalDate,
        zone: ZoneId = ZoneId.systemDefault()
    ): Pair<LocalDateTime, LocalDateTime> {
        val systemZone = ZoneId.systemDefault()
        val start = startDate.atStartOfDay(zone).toInstant()
        val endExclusive = endDate.plusDays(1).atStartOfDay(zone).toInstant()
        return LocalDateTime.ofInstant(start, systemZone) to LocalDateTime.ofInstant(endExclusive, systemZone)
    }

    fun getCalendarDateRange(
        startDate: String,
        endDate: String,
        timeZone: String? = null
    ): Pair<LocalDateTime, LocalDateTime> {
        return getCalendarDateRange(
            startDate = LocalDate.parse(startDate),
            endDate = LocalDate.parse(endDate),
            zone = resolveZoneId(timeZone)
        )
    }

    private fun resolveZoneId(timeZone: String?): ZoneId {
        if (timeZone.isNullOrBlank()) {
            return ZoneId.systemDefault()
        }
        return try {
            ZoneId.of(timeZone)
        } catch (_: Exception) {
            ZoneId.systemDefault()
        }
    }
}
