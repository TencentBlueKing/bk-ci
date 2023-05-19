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

package com.tencent.devops.common.event.util

import org.slf4j.LoggerFactory
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Enumeration

object IPUtils {

    private val logger = LoggerFactory.getLogger(IPUtils::class.java)

    fun getInnerIP(): String {
        val ipMap = getNtNameAndIP()
        var innerIp = ipMap["eth1"]
        if (innerIp.isNullOrBlank()) {
            logger.error("eth1 NIC IP is empty, therefore, get eth0's NIC")
            innerIp = ipMap["eth0"]
        }
        if (innerIp.isNullOrBlank()) {
            val ipSet = ipMap.entries
            for ((_, value) in ipSet) {
                innerIp = value
                if (!innerIp.isNullOrBlank()) {
                    break
                }
            }
        }

        return if (innerIp.isNullOrBlank()) "" else innerIp!!
    }

    fun getNtNameAndIP(): Map<String, String> {
        logger.info("#####################Start getNtNameAndIP")
        val allIp = HashMap<String, String>()

        try {
            val allNetInterfaces = NetworkInterface.getNetworkInterfaces() // 获取服务器的所有网卡
            if (null == allNetInterfaces) {
                logger.error("#####################getNtNameAndIP Can not get NetworkInterfaces")
            } else {
                loopNetworkIp(allNetInterfaces, allIp)
            }
        } catch (e: Exception) {
            logger.error("Failed to obtain NIC", e)
        }

        return allIp
    }

    private fun loopNetworkIp(netInterfaces: Enumeration<NetworkInterface>, allIp: HashMap<String, String>) {
        while (netInterfaces.hasMoreElements()) { // 循环网卡获取网卡的IP地址
            val netInterface = netInterfaces.nextElement()
            val netInterfaceName = netInterface.name
            // 过滤掉127.0.0.1的IP
            if (netInterfaceName.isNullOrBlank() || "lo".equals(netInterfaceName, ignoreCase = true)) {
                logger.info("loopback address or NIC name is empty")
                continue
            }

            val addresses = netInterface.inetAddresses
            while (addresses.hasMoreElements()) {
                val ip = addresses.nextElement() as InetAddress
                if (ip is Inet4Address && !ip.isLoopbackAddress) {
                    val machineIp = ip.hostAddress
                    logger.info("###############netInterfaceName=$netInterfaceName The Macheine IP=$machineIp")
                    allIp[netInterfaceName] = machineIp
                }
            }
        }
    }
}
