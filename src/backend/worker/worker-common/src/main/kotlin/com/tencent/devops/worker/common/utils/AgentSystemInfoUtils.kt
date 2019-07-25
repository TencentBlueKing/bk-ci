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

package com.tencent.devops.worker.common.utils

import com.tencent.devops.worker.common.env.AgentEnv
import org.slf4j.LoggerFactory
import oshi.SystemInfo
import oshi.hardware.CentralProcessor.TickType
import oshi.util.Util
import kotlin.math.roundToLong

object AgentSystemInfoUtils {
    private val logger = LoggerFactory.getLogger(AgentSystemInfoUtils::class.java)

    fun getAgentSystemInfo(): AgentSystemInfo {
        return AgentSystemInfo().apply {
            osType = AgentEnv.getOS().name
            cpuInfo = getAgentCpuInfo()
            memoryInfo = getAgentMemoryInfo()
            diskInfo = getAgentDiskInfo()
            networkInfo = getAgentNetworkInfo()
        }
    }

    fun getAgentDiskInfo(): List<AgentDiskInfo> {
        val systemInfo = SystemInfo()
        val hardware = systemInfo.hardware
        val start = System.currentTimeMillis()
        val startDiskStores = hardware.diskStores

        Util.sleep(2000)
        val timeDiff = (System.currentTimeMillis() - start) / 1000.0
        val endDiskStores = hardware.diskStores
        val endIoDataMap = mutableMapOf<String, Pair<Long, Long>>()

        endDiskStores.forEach { disk ->
            endIoDataMap.put(disk.name, Pair(disk.readBytes, disk.writeBytes))
        }

        val diskInfos = mutableListOf<AgentDiskInfo>()
        startDiskStores.forEach { disk ->
            if (disk.size == 0L) return@forEach
            val ioData = endIoDataMap.get(disk.name) ?: return@forEach
            val diskInfo = AgentDiskInfo().apply {
                name = disk.name
                serial = disk.serial
                size = disk.size
                readRate = ((ioData.first - disk.readBytes) / timeDiff).roundToLong()
                writeRate = ((ioData.second - disk.writeBytes) / timeDiff).roundToLong()
                println("r1: ${disk.readBytes}, r2: ${ioData.first}, w1: ${disk.writeBytes}, w1: ${ioData.second}")
            }

            diskInfos.add(diskInfo)
        }
        return diskInfos
    }

    fun getAgentNetworkInfo(): List<AgentNetworkInfo> {
        val systemInfo = SystemInfo()
        val hardware = systemInfo.getHardware()
        val networks = hardware.networkIFs

        val networkInfos = mutableListOf<AgentNetworkInfo>()
        networks.forEach { network ->
            if (network.iPv4addr.isEmpty()) return@forEach
            val hasData = (network.bytesRecv > 0 || network.bytesSent > 0 || network.packetsRecv > 0 ||
                network.packetsSent > 0)
            if (!hasData) return@forEach

            val start = System.currentTimeMillis()
            val startData = mutableListOf(0L, 0L, 0L, 0L)
            network.updateNetworkStats()
            startData[0] = network.bytesRecv
            startData[1] = network.bytesSent
            startData[2] = network.packetsRecv
            startData[3] = network.packetsSent

            Util.sleep(2000)
            val timeDiff = (System.currentTimeMillis() - start) / 1000.0
            network.updateNetworkStats()

            val endData = mutableListOf(0L, 0L, 0L, 0L)
            network.updateNetworkStats()
            endData[0] = network.bytesRecv
            endData[1] = network.bytesSent
            endData[2] = network.packetsRecv
            endData[3] = network.packetsSent

            val networkInfo = AgentNetworkInfo().apply {
                name = network.name
                displayName = network.displayName
                macAddr = network.macaddr
                mtu = network.mtu
                ipv4Addr = network.iPv4addr.toList()
                ipv6Addr = network.iPv6addr.toList()
                speed = network.speed

                bytesRecvRate = ((endData[0] - startData[0]) / timeDiff).roundToLong()
                bytesSentRate = ((endData[1] - startData[1]) / timeDiff).roundToLong()
                packetRecvRate = ((endData[2] - startData[2]) / timeDiff).roundToLong()
                packetSentRate = ((endData[3] - startData[3]) / timeDiff).roundToLong()
            }
            networkInfos.add(networkInfo)
        }
        return networkInfos
    }

    fun getAgentCpuInfo(): AgentCpuInfo {
        val systemInfo = SystemInfo()
        val hardware = systemInfo.getHardware()
        val processor = hardware.processor
        val physicalProcessorCount = processor.physicalProcessorCount
        val logicalProcessorCount = processor.logicalProcessorCount

        // 采集cpu使用率
//        var start = System.currentTimeMillis()
        val startTicks = processor.systemCpuLoadTicks
        Util.sleep(1000)
//        var timeDiff = (System.currentTimeMillis() - start) / 1000.0
        val endTicks = processor.systemCpuLoadTicks

        val userTick = endTicks[TickType.USER.index] - startTicks[TickType.USER.index]
        val niceTick = endTicks[TickType.NICE.index] - startTicks[TickType.NICE.index]
        val sysTick = endTicks[TickType.SYSTEM.index] - startTicks[TickType.SYSTEM.index]
        val idleTick = endTicks[TickType.IDLE.index] - startTicks[TickType.IDLE.index]
        val iowaitTick = endTicks[TickType.IOWAIT.index] - startTicks[TickType.IOWAIT.index]
        val irqTick = endTicks[TickType.IRQ.index] - startTicks[TickType.IRQ.index]
        val softirqTick = endTicks[TickType.SOFTIRQ.index] - startTicks[TickType.SOFTIRQ.index]
        val totalCpuTick = (userTick + niceTick + sysTick + idleTick + iowaitTick + irqTick + softirqTick).toDouble()
        val cpuTicks = CpuTickInfo(
            userTick / totalCpuTick,
            niceTick / totalCpuTick,
            sysTick / totalCpuTick,
            idleTick / totalCpuTick,
            iowaitTick / totalCpuTick,
            irqTick / totalCpuTick,
            softirqTick / totalCpuTick
        )
        val tickCpuLoad = processor.systemCpuLoadBetweenTicks
        val systemCpuLoad = processor.systemCpuLoad

        val processorLoads = processor.processorCpuLoadBetweenTicks.toList()

        return AgentCpuInfo(
            logicalProcessorCount,
            physicalProcessorCount,
            systemCpuLoad,
            tickCpuLoad,
            cpuTicks,
            processorLoads
        )
    }

    fun getAgentMemoryInfo(): AgentMemoryInfo {
        val systemInfo = SystemInfo()
        val hardware = systemInfo.getHardware()
        val memory = hardware.memory

        return AgentMemoryInfo(
            memory.total,
            memory.available,
            memory.swapTotal,
            memory.swapUsed
        )
    }
}