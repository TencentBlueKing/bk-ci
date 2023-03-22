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

package com.tencent.devops.process.util

import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessCode.BK_ETH1_NETWORK_CARD_IP_EMPTY
import com.tencent.devops.process.constant.ProcessCode.BK_FAILED_GET_NETWORK_CARD
import com.tencent.devops.process.constant.ProcessCode.BK_LOOPBACK_ADDRESS_OR_NIC_EMPTY
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

@Suppress("LongMethod", "NestedBlockDepth")
object CommonUtils {

    private val logger = LoggerFactory.getLogger(CommonUtils::class.java)

    fun getInnerIP(): String {
        val ipMap = getMachineIP()
        var innerIp = ipMap["eth1"]
        if (StringUtils.isBlank(innerIp)) {
            logger.info(
                MessageUtil.getMessageByLocale(
                messageCode = BK_ETH1_NETWORK_CARD_IP_EMPTY,
                language = I18nUtil.getDefaultLocaleLanguage()
            ))
            innerIp = ipMap["eth0"]
        }
        if (StringUtils.isBlank(innerIp)) {
            val ipSet = ipMap.entries
            for ((_, value) in ipSet) {
                innerIp = value
                if (!StringUtils.isBlank(innerIp)) {
                    break
                }
            }
        }

        return if (StringUtils.isBlank(innerIp) || null == innerIp) "" else innerIp
    }

    private fun getMachineIP(): Map<String, String> {
        logger.info("#####################Start getMachineIP")
        val allIp = HashMap<String, String>()

        try {
            val allNetInterfaces = NetworkInterface.getNetworkInterfaces() // 获取服务器的所有网卡
            if (null == allNetInterfaces) {
                logger.warn("#####################getMachineIP Can not get NetworkInterfaces")
            } else {
                while (allNetInterfaces.hasMoreElements()) { // 循环网卡获取网卡的IP地址
                    val netInterface = allNetInterfaces.nextElement()
                    val netInterfaceName = netInterface.name
                    if (StringUtils.isBlank(netInterfaceName) || "lo".equals(netInterfaceName, ignoreCase = true)) {
                        // 过滤掉127.0.0.1的IP
                        logger.info(
                            MessageUtil.getMessageByLocale(
                                messageCode = BK_LOOPBACK_ADDRESS_OR_NIC_EMPTY,
                                language = I18nUtil.getDefaultLocaleLanguage()
                            )
                        )
                    } else {
                        val addresses = netInterface.inetAddresses
                        while (addresses.hasMoreElements()) {
                            val ip = addresses.nextElement() as InetAddress
                            if (ip is Inet4Address && !ip.isLoopbackAddress) {
                                val machineIp = ip.hostAddress
                                logger.info(
                                    "###############netInterfaceName=$netInterfaceName The Macheine IP=$machineIp"
                                )
                                allIp[netInterfaceName] = machineIp
                            }
                        }
                    }
                }
            }
        } catch (ignore: Exception) {
            logger.warn(
                MessageUtil.getMessageByLocale(
                    messageCode = BK_FAILED_GET_NETWORK_CARD,
                    language = I18nUtil.getDefaultLocaleLanguage()
                ), ignore)
        }

        return allIp
    }
}
