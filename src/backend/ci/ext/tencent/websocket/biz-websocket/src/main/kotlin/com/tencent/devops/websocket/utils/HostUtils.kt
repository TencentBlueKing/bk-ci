package com.tencent.devops.websocket.utils

import org.slf4j.LoggerFactory
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.URL

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

    private const val DEFAULT_IP = "127.0.0.1"
    private val logger = LoggerFactory.getLogger(javaClass)

}

//fun main(argv: Array<String>) {
//    println(HostUtils.getHostIp("http://v2.devops.oa.com"))
//}