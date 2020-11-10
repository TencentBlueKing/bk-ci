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

import org.hyperic.sigar.FileSystem
import org.hyperic.sigar.FileSystemUsage
import org.hyperic.sigar.Sigar
import org.slf4j.LoggerFactory
import java.nio.charset.Charset
import java.util.ArrayDeque
import kotlin.math.roundToInt

object SigarUtil {
    private val logger = LoggerFactory.getLogger(SigarUtil::class.java)

    private const val MAX_MEM = 90
    private const val MAX_CPU = 90

    private val memQueue = ArrayDeque<Int>()
    private val cpuQueue = ArrayDeque<Int>()
    private val diskQueue = ArrayDeque<Int>()
    private val diskIOQueue = ArrayDeque<Int>()

    private const val queueMaxSize = 20

    private var queueMemValueSum = 0
    private var queueCpuValueSum = 0
    private var queueDiskValueSum = 0
    private var queueDiskIOValueSum = 0

    private val longMemQueue = ArrayDeque<Int>()
    private val longCpuQueue = ArrayDeque<Int>()
    private val longDiskIOQueue = ArrayDeque<Int>()

    private const val longQueueMaxSize = 30

    private var longQueueMemValueSum = 0
    private var longQueueCpuValueSum = 0
    private var longQueueDiskIOValueSum = 0

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

    fun getAverageDiskLoad(): Int {
        return try {
            queueDiskValueSum / diskQueue.size
        } catch (e: Exception) {
            0
        }
    }

    fun getAverageDiskIOLoad(): Int {
        return try {
            queueDiskIOValueSum / diskIOQueue.size
        } catch (e: Exception) {
            0
        }
    }

    fun getAverageLongMemLoad(): Int {
        return try {
            longQueueMemValueSum / longMemQueue.size
        } catch (e: Exception) {
            0
        }
    }

    fun getAverageLongCpuLoad(): Int {
        return try {
            longQueueCpuValueSum / longCpuQueue.size
        } catch (e: Exception) {
            0
        }
    }

    fun pushMem() {
        try {
            val element = getMemUsedPercent()
            if (memQueue.size >= queueMaxSize) {
                queueMemValueSum -= memQueue.pollLast()
            }

            memQueue.push(element)
            queueMemValueSum += element

            if (longMemQueue.size >= longQueueMaxSize) {
                longQueueMemValueSum -= longMemQueue.pollLast()
            }

            longMemQueue.push(element)
            longQueueMemValueSum += element
        } catch (e: Exception) {
            logger.error("push mem error.", e)
        }
    }

    fun pushCpu() {
        try {
            val element = getCpuUsedPercent()
            if (cpuQueue.size >= queueMaxSize) {
                queueCpuValueSum -= cpuQueue.pollLast()
            }

            cpuQueue.push(element)
            queueCpuValueSum += element

            if (longCpuQueue.size >= longQueueMaxSize) {
                longQueueCpuValueSum -= longCpuQueue.pollLast()
            }

            longCpuQueue.push(element)
            longQueueCpuValueSum += element
        } catch (e: Exception) {
            logger.error("push cpu error.", e)
        }
    }

    fun pushDisk() {
        try {
            val element = getDiskUsedPercent()
            if (diskQueue.size >= queueMaxSize) {
                queueDiskValueSum -= diskQueue.pollLast()
            }

            diskQueue.push(element)
            queueDiskValueSum += element
        } catch (e: Exception) {
            logger.error(" push disk error.", e)
        }
    }

    fun pushDiskIOUtil() {
        try {
            val element = getDiskIORate()
            // logger.info("push disk ioUtil element: $element")
            if (diskIOQueue.size >= queueMaxSize) {
                queueDiskIOValueSum -= diskIOQueue.pollLast()
            }

            diskIOQueue.push(element)
            queueDiskIOValueSum += element
        } catch (e: Exception) {
            logger.error("push disk ioUtil error.", e)
        }
    }

    fun getMemQueue(): ArrayDeque<Int> {
        return memQueue
    }

    private fun getMemUsedPercent(): Int {
        val sigar = Sigar()
        val mem = sigar.mem
/*        logger.info("usedPercent: " + mem.usedPercent)
        logger.info("used: " + mem.used)
        logger.info("total: " + mem.total)
        logger.info("actualUsed: " + mem.actualUsed)
        logger.info("actualFree: " + mem.actualFree)
        logger.info("free: " + mem.free)*/
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

    private fun getDiskUsedPercent(): Int {
        var element = file()
        // logger.info("getDiskUsedPercent ==========>：$element")
        if (element !in 0..100) {
            element = 0
        }
        return element
    }

    @Throws(Exception::class)
    private fun file(): Int {
        var diskUsedPercent = 0
        val sigar = Sigar()
        val fslist: Array<FileSystem> = sigar.fileSystemList
        for (i in fslist.indices) {
            val fs: FileSystem = fslist[i]
            var usage: FileSystemUsage?
            usage = sigar.getFileSystemUsage(fs.dirName)
            when (fs.type) {
                2 -> {
                    // 分区的盘符名称
                    // logger.info("盘符名称:    " + fs.devName)
                    // 分区的盘符名称
                    // logger.info("盘符路径:    " + fs.dirName)
                    if (fs.dirName == "/data") {
                        diskUsedPercent = (usage.usePercent * 100).roundToInt()
                    }
                }
            }
        }

        return diskUsedPercent
    }

    private fun getDiskIORate(): Int {
        var totalIOUtil = 0
        val commandStr = runCommand("iostat -d -x -k 1 8")
        val stringArray = commandStr!!.split("\n")
        stringArray.forEach {
            if (it.isNotEmpty() && !it.contains("Device") && !it.contains("Linux")) {
                val strArr = it.split(" ")
                val ioUtil = (strArr[strArr.size - 1].toDouble() * 100).roundToInt()
                // logger.info("====: $it || $ioUtil")
                totalIOUtil += ioUtil
            }
        }

        // logger.info("totalIOUtil: $totalIOUtil")
        var element = totalIOUtil / 800
        if (element !in 0..100) {
            element = 0
        }
        return element
    }

    /**
     * 执行系统命令
     *
     * @param CMD 命令
     * @return 字符串结果
     */
    private fun runCommand(CMD: String): String? {
        return try {
            val pos = Runtime.getRuntime().exec(CMD)
            pos.waitFor()
            pos.inputStream.readBytes().toString(Charset.defaultCharset())
        } catch (e: Exception) {
            logger.error("runCommand error.", e)
            ""
        }
    }
}
