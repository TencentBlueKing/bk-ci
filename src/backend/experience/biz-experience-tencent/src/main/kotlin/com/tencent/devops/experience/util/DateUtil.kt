package com.tencent.devops.experience.util

import java.time.LocalDateTime

object DateUtil {
    fun isExpired(endDate: LocalDateTime): Boolean {
        val today = today()
        return isExpired(endDate, today)
    }

    fun isExpired(endDate: LocalDateTime, today: LocalDateTime): Boolean {
        return endDate.isBefore(today)
    }

    fun today(): LocalDateTime {
        val now = LocalDateTime.now()
        return now.minusNanos(now.nano.toLong()).minusSeconds(now.second.toLong())
                .minusMinutes(now.minute.toLong()).minusHours(now.hour.toLong())
    }
}