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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.dockerhost.utils

import org.hyperic.sigar.Sigar
import java.util.ArrayDeque
import kotlin.math.roundToInt

object SigarUtil {

    private const val MAX_MEM = 90

    private const val MAX_CPU = 90

    private val memQueue = ArrayDeque<Int>()

    private val cpuQueue = ArrayDeque<Int>()

    private const val queueMaxSize = 10

    private var queueMemValueSum = 0

    private var queueCpuValueSum = 0

    fun loadEnable(): Boolean {
        return try {
            val averageMemLoad = queueMemValueSum / memQueue.size
            val averageCpuLoad = queueCpuValueSum / cpuQueue.size
            averageMemLoad < MAX_MEM && averageCpuLoad < MAX_CPU
        } catch (e: Exception) {
            true
        }
    }

    fun getAverageMemLoad(): Int {
        return try {
            queueMemValueSum / memQueue.size
        } catch (e: Exception) {
            0
        }
    }

    fun getAverageCpuLoad(): Int {
        return try {
            queueCpuValueSum / cpuQueue.size
        } catch (e: Exception) {
            0
        }
    }

    fun pushMem() {
        val element = getMemUsedPercent()
        if (memQueue.size >= queueMaxSize) {
            queueMemValueSum -= memQueue.pollLast()
        }

        memQueue.push(element)
        queueMemValueSum += element
    }

    fun pushCpu() {
        val element = getCpuUsedPercent()
        if (cpuQueue.size >= queueMaxSize) {
            queueCpuValueSum -= cpuQueue.pollLast()
        }

        cpuQueue.push(element)
        queueCpuValueSum += element
    }

    fun getMemQueue(): ArrayDeque<Int> {
        return memQueue
    }

    private fun getMemUsedPercent(): Int {
        val sigar = Sigar()
        val mem = sigar.mem
        val element = (mem.usedPercent).roundToInt()
        return if (element in 0..100) {
            element
        } else {
            0
        }
    }

    private fun getCpuUsedPercent(): Int {
        val sigar = Sigar()
        val cpuInfoList = sigar.cpuInfoList
        val cpuList = sigar.cpuPercList

        var cpuTotalIdle = 0.0
        for (i in cpuInfoList.indices) {
            val cpuPerc = cpuList[i]
            cpuTotalIdle += cpuPerc.idle
        }

        val element = 100 - (cpuTotalIdle / cpuInfoList.size.toDouble() * 100).toInt()
        return if (element in 0..100) {
            element
        } else {
            0
        }
    }
}
