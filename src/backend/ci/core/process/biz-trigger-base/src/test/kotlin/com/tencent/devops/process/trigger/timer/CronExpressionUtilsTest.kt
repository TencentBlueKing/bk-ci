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

package com.tencent.devops.process.trigger.timer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Calendar

class CronExpressionUtilsTest {

    @Test
    fun getRecentTriggerTime() {
        // */5 30 11：每天 11:30 内每 5 秒一次。下一次触发取决于当前时间，
        // 跑在 11:30 这一分钟里时第一次不是 11:30:00，不能把时刻写死。
        val cron = "*/5 30 11 * * ? *"
        val recentTriggerTimes = CronExpressionUtils.getRecentTriggerTime(
            cron = cron,
            numTimes = 2
        )
        assertEquals(2, recentTriggerTimes.size)
        recentTriggerTimes.forEach { date ->
            val calendar = Calendar.getInstance().apply { time = date }
            assertEquals(11, calendar.get(Calendar.HOUR_OF_DAY))
            assertEquals(30, calendar.get(Calendar.MINUTE))
            assertEquals(0, calendar.get(Calendar.SECOND) % 5)
        }
        val interval = recentTriggerTimes[1].time - recentTriggerTimes[0].time
        // 仍在 11:30 内：相邻两次相差 5 秒；已过该分钟最后一档：下一次是次日 11:30:00
        assertTrue(interval == 5_000L || interval >= 23 * 60 * 60 * 1000)
    }

    @Test
    fun isValidTimeInterval() {
        val cron = "* * /2 * * ?"
        assertFalse(CronExpressionUtils.isValidTimeInterval(cron))
    }
}
