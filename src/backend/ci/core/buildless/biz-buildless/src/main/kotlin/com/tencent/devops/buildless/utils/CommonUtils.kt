/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.buildless.utils

import org.slf4j.LoggerFactory
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.UnknownHostException
import java.util.Enumeration

object CommonUtils {

    private val logger = LoggerFactory.getLogger(CommonUtils::class.java)

    private var hostIp: String? = null

    fun formatContainerId(containerId: String): String {
        return if (containerId.length > 12) {
            containerId.substring(0, 12)
        } else {
            containerId
        }
    }

    fun getHostIp(): String {
        if (hostIp.isNullOrBlank()) {
            synchronized(this) {
                if (hostIp.isNullOrBlank()) {
                    hostIp = getInnerIP()
                    if (hostIp.isNullOrBlank()) {
                        throw RuntimeException("Empty host ip.")
                    }
                    logger.info("Get the HostIp($hostIp)")
                }
            }
        }
        return hostIp!!
    }

    private fun getInnerIP(localIp: String? = ""): String {
        if (localIp != null && localIp.isNotBlank()) {
            return localIp
        }

        val ipByNiList = getLocalIp4AddressFromNetworkInterface()
        return when {
            ipByNiList.isEmpty() -> {
                getIpBySocket()?.hostAddress ?: ""
            }
            ipByNiList.size == 1 -> {
                ipByNiList[0].hostAddress
            }
            else -> {
                getIpBySocket()?.hostAddress ?: ipByNiList[0].hostAddress
            }
        }
    }

    private fun getLocalIp4AddressFromNetworkInterface(): List<Inet4Address> {
        val addresses: MutableList<Inet4Address> = ArrayList()
        val e = NetworkInterface.getNetworkInterfaces() ?: return addresses
        while (e.hasMoreElements()) {
            val n = e.nextElement() as NetworkInterface
            if (!isValidInterface(n)) {
                continue
            }
            val ee: Enumeration<*> = n.inetAddresses
            while (ee.hasMoreElements()) {
                val i = ee.nextElement() as InetAddress
                if (isValidAddress(i)) {
                    addresses.add(i as Inet4Address)
                }
            }
        }
        return addresses
    }

    /**
     * 过滤回环网卡、点对点网卡、非活动网卡、虚拟网卡并要求网卡名字是eth或ens开头
     *
     * @param ni 网卡
     * @return 如果满足要求则true，否则false
     */
    private fun isValidInterface(ni: NetworkInterface): Boolean {
        return (!ni.isLoopback && !ni.isPointToPoint && ni.isUp && !ni.isVirtual &&
                (ni.name.startsWith("eth") || ni.name.startsWith("ens")))
    }

    /**
     * 判断是否是IPv4，并过滤回环地址.
     */
    private fun isValidAddress(address: InetAddress): Boolean {
        return address is Inet4Address && !address.isLoopbackAddress()
    }

    /**
     * 建立一个socket链接，当存在多网卡时会返回合适的出口IP，8.8.8.8是个测试地址 not needed to be reachable
     */
    private fun getIpBySocket(): Inet4Address? {
        try {
            DatagramSocket().use { socket ->
                socket.connect(InetAddress.getByName("8.8.8.8"), 10002)
                if (socket.localAddress is Inet4Address) {
                    return socket.localAddress as Inet4Address
                }
            }
        } catch (e: UnknownHostException) {
            return null
        }

        return null
    }
}
