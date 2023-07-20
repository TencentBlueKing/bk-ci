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

package com.tencent.devops.common.service.utils

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.service.PROFILE_AUTO
import com.tencent.devops.common.service.PROFILE_DEFAULT
import com.tencent.devops.common.service.PROFILE_DEVELOPMENT
import com.tencent.devops.common.service.PROFILE_PRODUCTION
import com.tencent.devops.common.service.PROFILE_STREAM
import com.tencent.devops.common.service.PROFILE_TEST
import com.tencent.devops.common.service.Profile
import java.io.File
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.util.Enumeration
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

object CommonUtils {

    const val ZH_CN = "ZH_CN" // 简体中文

    const val ZH_TW = "ZH_TW" // 台湾繁体中文

    private const val EN = "EN" // 英文

    private const val ZH_HK = "ZH_HK" // 香港繁体中文

    private val simpleCnLanList = listOf(ZH_CN, "ZH-CN")

    private val twCnLanList = listOf(ZH_TW, "ZH-TW", ZH_HK, "ZH-HK")

    private val logger = LoggerFactory.getLogger(CommonUtils::class.java)

    fun getInnerIP(): String {
        val ipMap = getMachineIP()
        var innerIp = ipMap["eth1"]
        if (StringUtils.isBlank(innerIp)) {
            logger.error("eth1 NIC IP is empty, therefore, get eth0's NIC IP")
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
            logger.error("Failed to obtain NIC", e)
        } catch (ignore: NullPointerException) {
            logger.error("Failed to obtain NIC", ignore)
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
            logger.info("The loopback address or NIC name is empty")
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
        fileChannelType: String,
        logo: Boolean = false,
        language: String
    ): Result<String?> {
        val serviceUrl = "$serviceUrlPrefix/service/artifactories/file/upload" +
                "?userId=$userId&fileChannelType=$fileChannelType&logo=$logo"
        logger.info("the serviceUrl is:$serviceUrl")
        OkhttpUtils.uploadFile(serviceUrl, file).use { response ->
            val responseContent = response.body!!.string()
            logger.error("uploadFile responseContent is: $responseContent")
            if (!response.isSuccessful) {
                val message = MessageUtil.getMessageByLocale(
                    messageCode = CommonMessageCode.SYSTEM_ERROR,
                    language = language
                )
                Result(CommonMessageCode.SYSTEM_ERROR.toInt(), message, null)
            }
            return JsonUtil.to(responseContent, object : TypeReference<Result<String?>>() {})
        }
    }

    /**
     * 获取语言信息
     * @return local语言信息
     */
    private fun getOriginLocale(): String {
        // 从request请求中获取本地语言信息
        val attributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
        return if (null != attributes) {
            val request = attributes.request
            val cookieLan = CookieUtil.getCookieValue(request, "blueking_language")
            cookieLan ?: LocaleContextHolder.getLocale().toString() // 获取字符集（与http请求头中的Accept-Language有关）
        } else {
            ZH_CN // 取不到语言信息默认为中文
        }
    }

    /**
     * 获取蓝盾能处理的语言信息
     * @return 蓝盾语言信息
     */
    fun getBkLocale(): String {
        val locale = getOriginLocale()
        return when {
            simpleCnLanList.contains(locale.toUpperCase()) -> ZH_CN // 简体中文
            twCnLanList.contains(locale.toUpperCase()) -> ZH_TW // 繁体中文
            else -> EN // 英文描述
        }
    }

    /**
     * 获取字符串的头部指定长度
     */
    fun interceptStringInLength(string: String?, length: Int): String? {
        return if (string != null && string.length > length) {
            string.substring(0, length - 1)
        } else string
    }

    /**
     * 把字符串转换成数组对象
     * @param str 字符串
     * @return 数组对象
     */
    fun strToList(str: String): List<String> {
        val dataList = mutableListOf<String>()
        if (str.contains(Regex("^\\s*\\[[\\w\\s\\S\\W]*]\\s*$"))) {
            dataList.addAll(JsonUtil.to(str))
        } else if (str.isNotBlank()) {
            dataList.add(str)
        }
        return dataList
    }

    /**
     * 获取db集群名称
     */
    fun getDbClusterName(): String {
        val profile = SpringContextUtil.getBean(Profile::class.java)
        return when {
            profile.isDev() -> {
                PROFILE_DEVELOPMENT
            }
            profile.isTest() -> {
                PROFILE_TEST
            }
            profile.isProd() -> {
                when {
                    profile.isAuto() -> {
                        PROFILE_AUTO
                    }
                    profile.isStream() -> {
                        PROFILE_STREAM
                    }
                    else -> {
                        PROFILE_PRODUCTION
                    }
                }
            }
            profile.isLocal() -> {
                PROFILE_DEFAULT
            }
            else -> {
                PROFILE_PRODUCTION
            }
        }
    }
}
