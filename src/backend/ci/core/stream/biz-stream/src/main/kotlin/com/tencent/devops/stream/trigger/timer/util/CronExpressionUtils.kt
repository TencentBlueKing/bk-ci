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

package com.tencent.devops.stream.trigger.timer.util

import org.quartz.TriggerUtils
import org.quartz.impl.triggers.CronTriggerImpl
import java.util.Date

object CronExpressionUtils {

    // 有效时间间隔60s
    private const val VALID_TIME_INTERVAL = 60 * 1000

    /**
     * 计算cron表达式接下来的执行时间
     * @param cron cron表达式
     * @param numTimes 输出几次
     */
    fun getRecentTriggerTime(
        cron: String,
        numTimes: Int = 2
    ): List<Date> {
        val trigger = CronTriggerImpl()
        trigger.cronExpression = cron
        return TriggerUtils.computeFireTimes(trigger, null, numTimes)
    }

    /**
     * 为了防止流水线执行太频繁，需要控制cron表达式执行时间间隔，不能秒级执行
     */
    fun isValidTimeInterval(cron: String): Boolean {
        val recentTriggerTimes = getRecentTriggerTime(cron)
        // 如果最近2次执行时间为空，表示表达式已经不会运行
        if (recentTriggerTimes.isEmpty() || recentTriggerTimes.size == 1) {
            return true
        }
        val interval = recentTriggerTimes[1].time - recentTriggerTimes[0].time
        return interval >= VALID_TIME_INTERVAL
    }
}
