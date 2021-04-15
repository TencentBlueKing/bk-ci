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

package com.tencent.bkrepo.common.storage.monitor

import com.tencent.bkrepo.common.api.util.HumanReadable
import com.tencent.bkrepo.common.api.util.HumanReadable.NANOS_PER_SECOND
import java.time.Duration
import java.time.temporal.ChronoUnit

data class Throughput(
    val bytes: Long,
    val time: Long,
    val unit: ChronoUnit = ChronoUnit.NANOS
) {
    val duration: Duration = Duration.of(time, unit)

    override fun toString(): String {
        with(HumanReadable) {
            val nanoTime = duration.toNanos()
            return "size: ${size(bytes)}, elapse: ${time(nanoTime)}, average: ${throughput(bytes, nanoTime)}"
        }
    }

    fun average(): Long {
        return (bytes.toDouble() / duration.toNanos() * NANOS_PER_SECOND).toLong()
    }

    companion object {
        val EMPTY = Throughput(0, 0)
    }
}

inline fun measureThroughput(bytes: Long, block: () -> Unit): Throughput {
    val start = System.nanoTime()
    block()
    val time = System.nanoTime() - start
    return Throughput(bytes = bytes, time = time)
}

inline fun measureThroughput(block: () -> Long): Throughput {
    val start = System.nanoTime()
    val bytes = block()
    val time = System.nanoTime() - start
    return Throughput(bytes = bytes, time = time)
}
