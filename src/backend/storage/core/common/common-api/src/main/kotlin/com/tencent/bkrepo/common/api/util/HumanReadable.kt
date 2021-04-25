/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.common.api.util

import java.text.DecimalFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

object HumanReadable {
    const val NANOS_PER_SECOND = 1000_000_000L
    private const val BYTES_PER_KB = 1024.0
    private val sizeUnits = arrayOf("B", "KB", "MB", "GB", "TB", "PB", "EB")

    fun size(bytes: Long): String {
        var size = bytes.toDouble()
        var index = 0
        while (size >= BYTES_PER_KB) {
            size /= BYTES_PER_KB
            index += 1
        }
        return "${DecimalFormat("0.##").format(size)} ${sizeUnits[index]}"
    }

    fun throughput(bytes: Long, nano: Long): String {
        val speed = bytes.toDouble() / nano * NANOS_PER_SECOND
        return size(speed.toLong()) + "/s"
    }

    fun time(nano: Long): String {
        val unit = chooseUnit(nano)
        val value = nano.toDouble() / TimeUnit.NANOSECONDS.convert(1, unit)
        return String.format(Locale.ROOT, "%.4g", value) + " " + abbreviate(unit)
    }

    fun time(time: Long, unit: TimeUnit): String {
        return time(unit.toNanos(time))
    }

    private fun chooseUnit(nano: Long): TimeUnit {
        return when {
            TimeUnit.DAYS.convert(nano, TimeUnit.NANOSECONDS) > 0 -> TimeUnit.DAYS
            TimeUnit.HOURS.convert(nano, TimeUnit.NANOSECONDS) > 0 -> TimeUnit.HOURS
            TimeUnit.MINUTES.convert(nano, TimeUnit.NANOSECONDS) > 0 -> TimeUnit.MINUTES
            TimeUnit.SECONDS.convert(nano, TimeUnit.NANOSECONDS) > 0 -> TimeUnit.SECONDS
            TimeUnit.MILLISECONDS.convert(nano, TimeUnit.NANOSECONDS) > 0 -> TimeUnit.MILLISECONDS
            TimeUnit.MICROSECONDS.convert(nano, TimeUnit.NANOSECONDS) > 0 -> TimeUnit.MICROSECONDS
            else -> TimeUnit.NANOSECONDS
        }
    }

    private fun abbreviate(unit: TimeUnit): String {
        return when (unit) {
            TimeUnit.NANOSECONDS -> "ns"
            TimeUnit.MICROSECONDS -> "μs"
            TimeUnit.MILLISECONDS -> "ms"
            TimeUnit.SECONDS -> "s"
            TimeUnit.MINUTES -> "min"
            TimeUnit.HOURS -> "h"
            TimeUnit.DAYS -> "d"
            else -> throw AssertionError()
        }
    }
}
