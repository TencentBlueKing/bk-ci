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

import com.github.dockerjava.api.model.AuthConfig
import com.tencent.devops.common.api.util.SecurityUtil
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.dockerhost.config.DockerHostConfig
import org.apache.commons.lang.StringUtils
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.Socket

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

    fun getInnerIP(): String {
        val ipMap = getMachineIP()
        var innerIp = ipMap["eth1"]
        if (StringUtils.isBlank(innerIp)) {
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
        val allIp = HashMap<String, String>()

        try {
            val allNetInterfaces = NetworkInterface.getNetworkInterfaces() // 获取服务器的所有网卡
            if (null == allNetInterfaces) {
                logger.error("#####################getMachineIP Can not get NetworkInterfaces")
            } else {
                while (allNetInterfaces.hasMoreElements()) { // 循环网卡获取网卡的IP地址
                    val netInterface = allNetInterfaces.nextElement()
                    val netInterfaceName = netInterface.name
                    if (StringUtils.isBlank(netInterfaceName) || "lo".equals(netInterfaceName, ignoreCase = true)) { // 过滤掉127.0.0.1的IP
//                        logger.info("loopback地址或网卡名称为空")
                    } else {
                        val addresses = netInterface.inetAddresses
                        while (addresses.hasMoreElements()) {
                            val ip = addresses.nextElement() as InetAddress
                            if (ip is Inet4Address && !ip.isLoopbackAddress) {
                                val machineIp = ip.hostAddress
                                allIp[netInterfaceName] = machineIp
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("获取网卡失败", e)
        }

        return allIp
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
                        Triple(str[0], imageNameStr.substringAfter(str[0] + "/").substringBefore(":" + nameTag[1]), nameTag[1])
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

    fun getAuthConfig(imageType: String?, dockerHostConfig: DockerHostConfig, imageName: String, registryUser: String?, registryPwd: String?): AuthConfig? {
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
