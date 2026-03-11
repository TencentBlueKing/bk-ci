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

package com.tencent.devops.environment.service.job

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.environment.dao.job.NetworkAreaDao
import com.tencent.devops.environment.utils.ComputeTimeUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.net.InetAddress
import java.nio.ByteBuffer
import java.time.LocalDateTime

@Service("AutoChooseAgentInstallChannelIdService")
class ChooseAgentInstallChannelIdService @Autowired constructor(
    private val dslContext: DSLContext,
    private val networkAreaDao: NetworkAreaDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ChooseAgentInstallChannelIdService::class.java)

        private const val PROD_NETWORK_AREA_SUPPORTING = 4
        private const val PROD_NETWORK_AREA_OSS = 6
        private const val PROD_NETWORK_AREA_DEVNET = 5 // 正式环境安装通道数值
        private val PROD_NETWORK_AREA_DEFAULT = null
    }

    fun autoChooseAgentInstallChannelId(ip: String): Int? {
        if (ip.isBlank()) {
            throw ParamBlankException(
                message = "innerIp or innerIpv6 must be selected."
            )
        }
        val networkAreaRecord = networkAreaDao.getOssAndDevnetNetAreaList(dslContext)
        val networkAreaMap = networkAreaRecord.map {
            it.netArea to it.netSegment.split(",")
        }.toMap()

        var networkArea = ""
        val startTime = LocalDateTime.now()
        if (ip.startsWith("9.134.") || ip.startsWith("9.135.")) {
            networkArea = "DEVNET"
        } else {
            networkAreaMap.forEach { (key, value) ->
                if (ipInRange(ip, value)) {
                    networkArea = key
                    return@forEach
                }
            }
        }
        val costTime = ComputeTimeUtils.calculateDuration(startTime, LocalDateTime.now())
        logger.info("Ip: $ip, Auto choose agent install channel: $networkArea, cost time:${costTime}s.")

        return when (networkArea) {
            "SUPPORTING" -> PROD_NETWORK_AREA_SUPPORTING // 实际走不到，该区域暂时无法判断
            "OSS" -> PROD_NETWORK_AREA_OSS
            "DEVNET" -> PROD_NETWORK_AREA_DEVNET
            else -> PROD_NETWORK_AREA_DEFAULT // IDC直连区域
        }
    }

    fun ipInRange(ip: String, subnets: List<String>): Boolean {
        // 将IP地址转换为长整型
        val targetIp = ipToLong(InetAddress.getByName(ip))

        // 遍历每个子网
        for (subnet in subnets) {
            val parts = subnet.split("/")
            val subnetIp = ipToLong(InetAddress.getByName(parts[0]))
            val prefix = parts[1].toInt()

            // 计算子网的最小IP和最大IP
            val minIp = subnetIp and ((0xFFFFFFFF.toInt() shl (32 - prefix)).toLong())
            val maxIp = minIp or ((0xFFFFFFFF.toInt() shr prefix).toLong())

            // 判断目标IP是否在子网范围内
            if (targetIp in minIp..maxIp) {
                return true
            }
        }
        return false
    }

    private fun ipToLong(inetAddress: InetAddress): Long {
        val buffer = ByteBuffer.wrap(inetAddress.address)
        var ip: Long = 0
        while (buffer.hasRemaining()) {
            ip = ip shl 8 or (buffer.get().toInt() and 0xFF).toLong()
        }
        return ip
    }
}