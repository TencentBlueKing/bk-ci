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

package com.tencent.devops.websocket.utils

import org.slf4j.LoggerFactory
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.URL

@Suppress("ALL")
object HostUtils {

    fun getHostIp(gateway: String?): String {
        try {
            val localHost = InetAddress.getLocalHost()
            return if (localHost.isLoopbackAddress) {
                getFromUDP(gateway) ?: DEFAULT_IP
            } else {
                localHost.hostAddress
            }
        } catch (e: Throwable) {
            logger.warn("Fail to get local host ip", e)
            try {
                return getFromUDP(gateway) ?: DEFAULT_IP
            } catch (t: Throwable) {
                logger.warn("Fail to use socket to get the localhost host")
            }
        }
        return "127.0.0.1"
    }

    private fun getFromUDP(gateway: String?): String? {
        if (gateway.isNullOrBlank()) {
            return null
        }

        val gatewayHost = try {
            val url = URL(gateway)
            url.host
        } catch (t: Throwable) {
            logger.warn("Fail to get the gateway host", t)
            return null
        }

        DatagramSocket().use { socket ->
            socket.connect(InetAddress.getByName(gatewayHost), 10002)
            return socket.localAddress.hostAddress
        }
    }

    fun getRealSession(query: String?): String? {
        if (query == null || query.isNullOrEmpty()) {
            return null
        }
        return query.substringAfter("sessionId=")?.substringBefore("&t=")
    }

    private const val DEFAULT_IP = "127.0.0.1"
    private val logger = LoggerFactory.getLogger(javaClass)
}
