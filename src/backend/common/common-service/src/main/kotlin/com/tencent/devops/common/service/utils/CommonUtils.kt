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

package com.tencent.devops.common.service.utils

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import org.apache.commons.lang.StringUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.util.Enumeration

object CommonUtils {

    private val logger = LoggerFactory.getLogger(CommonUtils::class.java)

    fun getInnerIP(): String {
        val ipMap = getMachineIP()
        var innerIp = ipMap["eth1"]
        if (StringUtils.isBlank(innerIp)) {
            logger.error("eth1 网卡Ip为空，因此，获取eth0的网卡ip")
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
        logger.info("####Start getMachineIP")
        val allIp = HashMap<String, String>()

        try {
            val allNetInterfaces = NetworkInterface.getNetworkInterfaces() // 获取服务器的所有网卡
            if (null == allNetInterfaces) {
                logger.error("####getMachineIP Can not get NetworkInterfaces")
            } else {
                while (allNetInterfaces.hasMoreElements()) { // 循环网卡获取网卡的IP地址
                    parseIp(allNetInterfaces, allIp)
                }
            }
        } catch (e: SocketException) {
            logger.error("获取网卡失败", e)
        } catch (e: NullPointerException) {
            logger.error("获取网卡失败", e)
        }

        return allIp
    }

    private fun parseIp(
        allNetInterfaces: Enumeration<NetworkInterface>,
        allIp: HashMap<String, String>
    ) {
        val netInterface = allNetInterfaces.nextElement()
        val netInterfaceName = netInterface.name
        // 过滤掉127.0.0.1的IP
        if (StringUtils.isBlank(netInterfaceName) || "lo".equals(netInterfaceName, ignoreCase = true)) {
            logger.info("loopback地址或网卡名称为空")
        } else {
            val addresses = netInterface.inetAddresses
            while (addresses.hasMoreElements()) {
                val ip = addresses.nextElement() as InetAddress
                if (ip is Inet4Address && !ip.isLoopbackAddress) {
                    val machineIp = ip.hostAddress
                    logger.info("####netInterfaceName=$netInterfaceName The Machine IP=$machineIp")
                    allIp[netInterfaceName] = machineIp
                }
            }
        }
    }

    fun serviceUploadFile(
        userId: String,
        serviceUrlPrefix: String,
        file: File,
        fileChannelType: String
    ): Result<String?> {
        val serviceUrl =
            "$serviceUrlPrefix/service/artifactories/file/upload?userId=$userId&fileChannelType=$fileChannelType"
        logger.info("the serviceUrl is:$serviceUrl")
        OkhttpUtils.uploadFile(serviceUrl, file).use { response ->
            val responseContent = response.body()!!.string()
            logger.error("uploadFile responseContent is: $responseContent")
            if (!response.isSuccessful) {
                return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.SYSTEM_ERROR)
            }
            return JsonUtil.to(responseContent, object : TypeReference<Result<String?>>() {})
        }
    }
}