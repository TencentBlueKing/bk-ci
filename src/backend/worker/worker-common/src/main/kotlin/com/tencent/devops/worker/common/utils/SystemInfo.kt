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

data class AgentSystemInfo(
    var hostName: String = "",
    var osType: String = "",
    var osVersion: String = "",
    var cpuInfo: AgentCpuInfo? = null,
    var memoryInfo: AgentMemoryInfo? = null,
    var diskInfo: List<AgentDiskInfo>? = null,
    var networkInfo: List<AgentNetworkInfo>? = null
)

data class AgentCpuInfo(
    var logicalProcessorCount: Int = 0,
    var physicalProcessorCount: Int = 0,
    var systemCpuLoad: Double = 0.0,
    var tickCpuLoad: Double = 0.0,
    var tickInfo: CpuTickInfo? = null,
    var processorLoads: List<Double> = listOf()
)

data class AgentMemoryInfo(
    var total: Long = 0L,
    var available: Long = 0L,
    var swapUsed: Long = 0L,
    var swapTotal: Long = 0L
)

data class AgentNetworkInfo(
    var name: String = "",
    var displayName: String = "",
    var macAddr: String = "",
    var mtu: Int = 0,
    var ipv4Addr: List<String> = listOf(),
    var ipv6Addr: List<String> = listOf(),
    var speed: Long = 0L,
    var bytesRecvRate: Long = 0L,
    var bytesSentRate: Long = 0L,
    var packetRecvRate: Long = 0L,
    var packetSentRate: Long = 0L
)

data class AgentDiskInfo(
    var name: String = "",
    var serial: String = "",
    var size: Long = 0L,
    var readRate: Long = 0L,
    var writeRate: Long = 0L
)

data class LogicDiskInfo(
    var name: String = "",
    var totalSpace: Long = 0L,
    var usableSpace: Long = 0L,
    var type: String = ""
)

data class CpuTickInfo(
    val user: Double,
    val nice: Double,
    val sys: Double,
    val idle: Double,
    val iowait: Double,
    val irq: Double,
    val softirq: Double
)