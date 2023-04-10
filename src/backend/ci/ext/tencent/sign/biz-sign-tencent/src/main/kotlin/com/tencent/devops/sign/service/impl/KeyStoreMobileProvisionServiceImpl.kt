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

package com.tencent.devops.sign.service.impl

import com.dd.plist.NSArray
import com.dd.plist.NSDictionary
import com.dd.plist.NSString
import com.dd.plist.PropertyListParser
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.script.CommandLineUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.sign.Constants.KEYSTORE_CATEGORY_PROVISION
import com.tencent.devops.sign.Constants.KEYSTORE_HTTP_HEADER_AUTH
import com.tencent.devops.sign.Constants.KEYSTORE_HTTP_HEADER_IP
import com.tencent.devops.sign.api.constant.SignMessageCode
import com.tencent.devops.sign.api.constant.SignMessageCode.BK_DESCRIPTION_FILE_FOR_CERTIFICATE
import com.tencent.devops.sign.api.constant.SignMessageCode.BK_FAILED_INSERT
import com.tencent.devops.sign.api.pojo.IpaSignInfo
import com.tencent.devops.sign.service.MobileProvisionService
import com.tencent.devops.sign.service.MobileProvisionService.Companion.KEYCHAIN_ACCESS_GROUPS_KEY
import com.tencent.devops.sign.service.MobileProvisionService.Companion.TEAM_IDENTIFIER_KEY
import com.tencent.devops.sign.utils.EncryptUtil
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.File
import java.net.InetAddress
import java.nio.charset.StandardCharsets
import java.time.Instant

@Suppress("NestedBlockDepth")
@Service
class
KeyStoreMobileProvisionServiceImpl @Autowired constructor() : MobileProvisionService {
    @Value("\${keystore.url:}")
    private val keyStoreUrl = ""

    @Value("\${keystore.certUrl:}")
    private val keyStoreCertUrl = "/api/auth/getCert?id=%s&category=%s"

    @Value("\${keystore.authId:}")
    private val keyStoreAuthId = ""

    @Value("\${keystore.authSecret:}")
    private val keyStoreAuthSecret = ""

    @Value("\${keystore.keyChainGroups:}")
    private val keyChainGroups: String = ""

    @Value("\${keystore.wildcardMobileProvision.certId:}")
    private var keyStoreCertId: String = ""

    @Value("\${keystore.wildcardMobileProvision.provisionId:}")
    private var keyStoreProvisionId: String = ""

    @Value("\${keystore.wildcardMobileProvision2.certId:}")
    private var keyStoreCertId2: String = ""

    @Value("\${keystore.wildcardMobileProvision2.provisionId:}")
    private var keyStoreProvisionId2: String = ""

    companion object {
        private val logger = LoggerFactory.getLogger(KeyStoreMobileProvisionServiceImpl::class.java)
        private var token: String? = null
    }

    override fun downloadMobileProvision(mobileProvisionDir: File, projectId: String, mobileProvisionId: String): File {
        // 从keystore下载文件
        val mobileProvisionFile = File("${mobileProvisionDir.canonicalPath}/$mobileProvisionId.mobileprovision")
        val url = String.format(keyStoreUrl + keyStoreCertUrl, mobileProvisionId, KEYSTORE_CATEGORY_PROVISION)
        val headers = mutableMapOf<String, String>()
        headers[KEYSTORE_HTTP_HEADER_AUTH] = token ?: ""
        headers[KEYSTORE_HTTP_HEADER_IP] = InetAddress.getLocalHost().hostAddress
        logger.info("Keystore download mobileprovision url:$url")
        logger.info("Keystore download mobileprovision header:$headers")
        OkhttpUtils.doGet(url, headers).use { resp ->
            if (resp.code != 200 || resp.body == null) {
                throw ErrorCodeException(
                    errorCode = SignMessageCode.ERROR_MP_NOT_EXIST,
                    defaultMessage = "KeyStore描述文件($mobileProvisionId)不存在或者下载失败。"
                )
            }
            val decryptedMobileProvisionEncrypt = resp.body!!.bytes()
            logger.info("Keystore decrypt decryptedMobileProvisionEncrypt:$decryptedMobileProvisionEncrypt")
            logger.info("Keystore decrypt keyStoreAuthSecret:$keyStoreAuthSecret")
            val decryptedMobileProvisionDecrypt = EncryptUtil.decrypt(
                encrypted = decryptedMobileProvisionEncrypt,
                key = keyStoreAuthSecret
            )
            mobileProvisionFile.writeBytes(decryptedMobileProvisionDecrypt)
        }
        return mobileProvisionFile
    }

    override fun handleEntitlement(entitlementFile: File, keyChainGroupsList: List<String>?) {
        val rootDict = PropertyListParser.parse(entitlementFile) as NSDictionary

        // 处理keychain-access-groups中无用的com.apple.token
        if (rootDict.containsKey(KEYCHAIN_ACCESS_GROUPS_KEY)) {
            val keychainArray = (rootDict.objectForKey(KEYCHAIN_ACCESS_GROUPS_KEY) as NSArray).array.withIndex()
            for ((index, e) in keychainArray) {
                if (e.toString() == "com.apple.token") {
                    val removeKeyChainGroupCMD =
                        "plutil -remove keychain-access-groups.$index ${entitlementFile.canonicalPath}"
                    CommandLineUtils.execute(removeKeyChainGroupCMD, entitlementFile.parentFile, true)
                    break
                }
            }
        }

        if (keyChainGroups.isBlank()) {
            throw ErrorCodeException(
                errorCode = SignMessageCode.ERROR_INSERT_KEYCHAIN_GROUPS,
                defaultMessage = "未找到配置keystore.keyChainGroups，请检查"
            )
        }
        val keyChainAccessGroupsList = keyChainGroups.split(";")
            .plus(keyChainGroupsList ?: emptyList())
        // 解析entitlement文件
        try {
            // entitlement
            if (rootDict.containsKey(TEAM_IDENTIFIER_KEY) && rootDict.containsKey(KEYCHAIN_ACCESS_GROUPS_KEY)) {
                val teamId = (rootDict.objectForKey(TEAM_IDENTIFIER_KEY) as NSString).toString()
                if (teamId.isNotBlank() && keyChainAccessGroupsList.isNotEmpty()) {
                    keyChainAccessGroupsList.forEach {
                        if (it.isNotBlank()) {
                            val insertKeyChainGroupCMD =
                                "plutil -insert keychain-access-groups.0 -string" +
                                    " '$teamId.$it' ${entitlementFile.canonicalPath}"
                            CommandLineUtils.execute(
                                command = insertKeyChainGroupCMD,
                                workspace = entitlementFile.parentFile,
                                print2Logger = true)
                        }
                    }
                }
            }
        } catch (ignore: Exception) {
            logger.warn(
                MessageUtil.getMessageByLocale(
                    messageCode = BK_FAILED_INSERT,
                    language = I18nUtil.getLanguage(I18nUtil.getRequestUserId()),
                    params = arrayOf(entitlementFile.canonicalPath)
                ), ignore)
            throw ErrorCodeException(
                errorCode = SignMessageCode.ERROR_INSERT_KEYCHAIN_GROUPS,
                defaultMessage = "entitlement插入keychain失败"
            )
        }
    }

    override fun downloadWildcardMobileProvision(mobileProvisionDir: File, ipaSignInfo: IpaSignInfo): File? {
        val wildcardMobileProvisionMap: MutableMap<String, String> = mutableMapOf()
        if (keyStoreCertId.isNotBlank() && keyStoreProvisionId.isNotBlank()) {
            wildcardMobileProvisionMap[keyStoreCertId.toLowerCase()] = keyStoreProvisionId
            wildcardMobileProvisionMap[keyStoreCertId.toUpperCase()] = keyStoreProvisionId
        }
        if (keyStoreCertId2.isNotBlank() && keyStoreProvisionId2.isNotBlank()) {
            wildcardMobileProvisionMap[keyStoreCertId2.toLowerCase()] = keyStoreProvisionId2
            wildcardMobileProvisionMap[keyStoreCertId2.toUpperCase()] = keyStoreProvisionId2
        }
        val wildcardMobileProvisionId = wildcardMobileProvisionMap[ipaSignInfo.certId.toUpperCase()]
        if (wildcardMobileProvisionId.isNullOrBlank()) {
            logger.warn(
                MessageUtil.getMessageByLocale(
                    messageCode = BK_DESCRIPTION_FILE_FOR_CERTIFICATE,
                    language = I18nUtil.getLanguage(I18nUtil.getRequestUserId()),
                    params = arrayOf(ipaSignInfo.certId)
                )
            )
            return null
        }
        return downloadMobileProvision(
            mobileProvisionDir = mobileProvisionDir,
            projectId = ipaSignInfo.projectId,
            mobileProvisionId = wildcardMobileProvisionId
        )
    }

    private fun generateToken() {
        val claims = mutableMapOf<String, Any>()
        claims["authId"] = keyStoreAuthId
        claims["timeMillis"] = Instant.now().epochSecond
        val key = Keys.hmacShaKeyFor(keyStoreAuthSecret.toByteArray(StandardCharsets.UTF_8))
        token = Jwts.builder().setClaims(claims)
            .signWith(SignatureAlgorithm.HS256, key).compact()
    }

    /*
    * keystore的jwt有效期为3分钟，设置为
    * */
    @Scheduled(fixedDelay = 1 * 60 * 1000)
    fun refreshToken() {
        logger.info("Refresh keystore jwt token")
        generateToken()
    }
}
