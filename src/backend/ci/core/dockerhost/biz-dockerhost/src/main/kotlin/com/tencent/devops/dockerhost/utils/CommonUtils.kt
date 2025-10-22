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

package com.tencent.devops.dockerhost.utils

import com.github.dockerjava.api.model.AuthConfig
import com.tencent.devops.common.api.util.SecurityUtil
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.dockerhost.config.DockerHostConfig
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.Socket
import java.net.UnknownHostException
import java.util.ArrayList
import java.util.Enumeration

object CommonUtils {

    private val logger = LoggerFactory.getLogger(CommonUtils::class.java)

    private const val dockerHubUrl = "https://index.docker.io/v1/"

    fun isPortUsing(host: String, port: Int): Boolean {
        return try {
            // 建立一个Socket连接
            val address = InetAddress.getByName(host)
            Socket(address, port)
            true
        } catch (e: IOException) {
            false
        }
    }

    fun getInnerIP(localIp: String? = ""): String {
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

    fun normalizeImageName(imageNameStr: String): String {
        val (url, name, tag) = parseImage(imageNameStr)
        return when (url) {
            dockerHubUrl -> "$name:$tag"
            else -> "$url/$name:$tag"
        }
    }

    private fun parseImage(imageNameInput: String): Triple<String, String, String> {
        val (url, name, tag) = parseImageWithoutTrim(imageNameInput)
        return Triple(url.trim(), name.trim(), tag.trim())
    }

    @Suppress("ALL")
    private fun parseImageWithoutTrim(imageNameInput: String): Triple<String, String, String> {
        val imageNameStr = imageNameInput.removePrefix("http://").removePrefix("https://")
        val arry = imageNameStr.split(":")
        if (arry.size == 1) {
            val str = imageNameStr.split("/")
            return if (str.size == 1) {
                Triple(dockerHubUrl, imageNameStr, "latest")
            } else {
                Triple(str[0], imageNameStr.substringAfter(str[0] + "/"), "latest")
            }
        } else if (arry.size == 2) {
            val str = imageNameStr.split("/")
            when {
                str.size == 1 -> return Triple(dockerHubUrl, arry[0], arry[1])
                str.size >= 2 -> return if (str[0].contains(":")) {
                    Triple(str[0], imageNameStr.substringAfter(str[0] + "/"), "latest")
                } else {
                    if (str.last().contains(":")) {
                        val nameTag = str.last().split(":")
                        Triple(str[0],
                            imageNameStr.substringAfter(str[0] + "/").substringBefore(":" + nameTag[1]),
                            nameTag[1])
                    } else {
                        Triple(str[0], str.last(), "latest")
                    }
                }
                else -> {
                    logger.error("image name invalid: $imageNameStr")
                    throw Exception("image name invalid.")
                }
            }
        } else if (arry.size == 3) {
            val str = imageNameStr.split("/")
            if (str.size >= 2) {
                val tail = imageNameStr.removePrefix(str[0] + "/")
                val nameAndTag = tail.split(":")
                if (nameAndTag.size != 2) {
                    logger.error("image name invalid: $imageNameStr")
                    throw Exception("image name invalid.")
                }
                return Triple(str[0], nameAndTag[0], nameAndTag[1])
            } else {
                logger.error("image name invalid: $imageNameStr")
                throw Exception("image name invalid.")
            }
        } else {
            logger.error("image name invalid: $imageNameStr")
            throw Exception("image name invalid.")
        }
    }

    fun getAuthConfig(
        imageType: String?,
        dockerHostConfig: DockerHostConfig,
        imageName: String,
        registryUser: String?,
        registryPwd: String?
    ): AuthConfig? {
        return if (imageType == ImageType.THIRD.type) {
            val (registryHost, _, _) = parseImage(imageName)
            logger.info("registry host: $registryHost")
            if (registryUser.isNullOrBlank()) {
                AuthConfig().withRegistryAddress(registryHost)
            } else {
                logger.info("registryUser: $registryUser, registryPwd: $registryPwd")
                AuthConfig()
                    .withUsername(registryUser)
                    .withPassword(registryPwd)
                    .withRegistryAddress(registryHost)
            }
        } else {
            AuthConfig()
                .withUsername(dockerHostConfig.registryUsername)
                .withPassword(SecurityUtil.decrypt(dockerHostConfig.registryPassword!!))
                .withRegistryAddress(dockerHostConfig.registryUrl)
        }
    }
}
