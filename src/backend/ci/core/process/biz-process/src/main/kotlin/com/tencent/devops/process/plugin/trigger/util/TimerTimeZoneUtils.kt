/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
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

package com.tencent.devops.process.plugin.trigger.util

import java.time.DateTimeException
import java.time.ZoneId
import java.util.TimeZone

/**
 * 定时触发时区工具。
 *
 * 调度语义：cron 按 IANA 墙上时间解释，与 JVM 部署时区解耦。
 * 存量无配置时统一回落 [DEFAULT_LEGACY_TIME_ZONE]（东八区 / 上海）。
 */
object TimerTimeZoneUtils {

    /** 存量定时任务 / DB 缺省时区：Asia/Shanghai（东八区） */
    const val DEFAULT_LEGACY_TIME_ZONE = "Asia/Shanghai"

    fun resolve(configured: String?): String {
        val raw = configured?.trim().orEmpty()
        if (raw.isEmpty()) {
            return DEFAULT_LEGACY_TIME_ZONE
        }
        return try {
            ZoneId.of(raw).id
        } catch (_: DateTimeException) {
            DEFAULT_LEGACY_TIME_ZONE
        }
    }

    fun toQuartzTimeZone(configured: String?): TimeZone {
        return TimeZone.getTimeZone(ZoneId.of(resolve(configured)))
    }
}
