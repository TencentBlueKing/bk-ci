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

package com.tencent.devops.process.engine.common

import com.tencent.devops.common.pipeline.EnvReplacementParser
import com.tencent.devops.process.utils.PipelineVarUtil
import java.util.concurrent.TimeUnit

object Timeout {

    const val DEFAULT_TIMEOUT_MIN = 900
    const val DEFAULT_PREPARE_MINUTES = 10 // 10分钟
    const val DEFAULT_STAGE_TIMEOUT_HOURS = 24 // 24小时
    private const val MAX_STAGE_REVIEW_DAYS = 30L // 审核最大天数
    const val MAX_JOB_RUN_DAYS = 7L // Job运行最大天数

    val MAX_HOURS = TimeUnit.DAYS.toHours(MAX_STAGE_REVIEW_DAYS) // 60 * 24 = 1440 小时 = 审核最多超时60天

    val STAGE_MAX_MILLS = TimeUnit.HOURS.toMillis(MAX_HOURS) + 1 // 毫秒+1

    val MAX_MINUTES = TimeUnit.DAYS.toMinutes(MAX_JOB_RUN_DAYS).toInt() // 7 * 24 * 60 = 10080 分钟 = 最多超时7天

    val CONTAINER_MAX_MILLS = TimeUnit.MINUTES.toMillis(MAX_MINUTES.toLong()) + 1 // 毫秒+1

    private fun transTimeoutObj(timeoutStr: String?): TimeoutObj {
        var change = false
        var minute = try {
            if (!timeoutStr.isNullOrBlank()) {
                timeoutStr.toInt()
            } else {
                change = true
                DEFAULT_TIMEOUT_MIN
            }
        } catch (badConfig: Exception) {
            change = true
            DEFAULT_TIMEOUT_MIN
        }
        if (minute <= 0 || minute > MAX_MINUTES) {
            change = true
            minute = MAX_MINUTES
        }
        return TimeoutObj(
            beforeChangeStr = timeoutStr,
            minutes = minute,
            millis = transMinuteTimeoutToMills(minute),
            change = change
        )
    }

    fun transMinuteTimeoutToSec(timeoutMinutes: Int?): Long {
        var minute = timeoutMinutes ?: DEFAULT_TIMEOUT_MIN
        if (minute <= 0 || minute > MAX_MINUTES) {
            minute = MAX_MINUTES
        }
        return TimeUnit.MINUTES.toSeconds(minute.toLong()) + 1 // #5109 buffer 1 second
    }

    fun transMinuteTimeoutToMills(timeoutMinutes: Int?): Long {
        var minute = timeoutMinutes ?: DEFAULT_TIMEOUT_MIN
        if (minute <= 0 || minute > MAX_MINUTES) {
            minute = MAX_MINUTES
        }
        return TimeUnit.MINUTES.toMillis(minute.toLong()) + 1 // #5109 buffer 1 second
    }

    /**
     * #7954 timeout支持变量解析
     */
    fun decTimeout(timeoutVar: String?, contextMap: Map<String, String>): TimeoutObj {

        val obj: TimeoutObj

        val timeoutStr = timeoutVar?.trim()

        if (PipelineVarUtil.isVar(timeoutStr)) { // 使用了变量的方式定义超时，需要解析

            val tTimeout = EnvReplacementParser.parse(timeoutStr, contextMap = contextMap)

            // 要检查配置的超时值是否在合理范围内
            obj = transTimeoutObj(tTimeout)

            if (tTimeout != timeoutStr) { // 发生了变量替换 ${{ xxx }} ==> 123
                obj.replaceByVar = true
            }
        } else { // 普通的方式，做了重置
            obj = transTimeoutObj(timeoutStr)
        }

        return obj
    }

    data class TimeoutObj(
        var beforeChangeStr: String?, // 原超时字符串形式 比如 123 或者 ${{ xxx }}
        var minutes: Int, // 超时分钟数
        var millis: Long, // 超时毫秒数
        var change: Boolean = false, // 是否有被修改过
        var replaceByVar: Boolean = false // 是否被变量替换成功
    )
}
