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

import com.dd.plist.NSDictionary
import com.dd.plist.NSString
import com.dd.plist.PropertyListParser
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.api.util.script.CommandLineUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.pojo.enums.GatewayType
import com.tencent.devops.sign.api.constant.SignMessageCode
import com.tencent.devops.sign.api.pojo.IpaSignInfo
import com.tencent.devops.sign.service.MobileProvisionService
import com.tencent.devops.sign.service.MobileProvisionService.Companion.KEYCHAIN_ACCESS_GROUPS_KEY
import com.tencent.devops.sign.service.MobileProvisionService.Companion.TEAM_IDENTIFIER_KEY
import com.tencent.devops.ticket.api.ServiceCertResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.util.Base64

@Suppress("NestedBlockDepth")
@Service
class MobileProvisionServiceImpl @Autowired constructor(
    private val client: Client
) : MobileProvisionService {

    companion object {
        private val logger = LoggerFactory.getLogger(MobileProvisionServiceImpl::class.java)
        private val pairKey = DHUtil.initKey()
        private val privateKey = pairKey.privateKey
        private val publicKey = String(Base64.getEncoder().encode(pairKey.publicKey))
    }

    override fun downloadMobileProvision(mobileProvisionDir: File, projectId: String, mobileProvisionId: String): File {
        // 从ticket模块获取描述文件
        val mpInfo = client.getGateway(
            clz = ServiceCertResource::class,
            gatewayType = GatewayType.DEVNET_PROXY
        ).getEnterprise(projectId = projectId, certId = mobileProvisionId, publicKey = publicKey).data
            ?: throw ErrorCodeException(errorCode = SignMessageCode.ERROR_MP_NOT_EXIST, defaultMessage = "描述文件不存在。")
        val publicKeyServer = Base64.getDecoder().decode(mpInfo.publicKey)
        val mpContent = Base64.getDecoder().decode(mpInfo.mobileProvisionContent)
        val mobileProvision = DHUtil.decrypt(mpContent, publicKeyServer, privateKey)
        val mobileProvisionFile = File("${mobileProvisionDir.canonicalPath}/$mobileProvisionId.mobileprovision")
        mobileProvisionFile.writeBytes(mobileProvision)
        return mobileProvisionFile
    }

    override fun handleEntitlement(entitlementFile: File, keyChainGroupsList: List<String>?) {
        if (keyChainGroupsList == null) return
        val rootDict = PropertyListParser.parse(entitlementFile) as NSDictionary
        // 解析entitlement文件
        try {
            // entitlement
            if (rootDict.containsKey(TEAM_IDENTIFIER_KEY) && rootDict.containsKey(KEYCHAIN_ACCESS_GROUPS_KEY)) {
                val teamId = (rootDict.objectForKey(TEAM_IDENTIFIER_KEY) as NSString).toString()
                if (teamId.isNotBlank() && keyChainGroupsList.isNotEmpty()) {
                    keyChainGroupsList.forEach {
                        if (it.isNotBlank()) {
                            val insertKeyChainGroupCMD =
                                "plutil -insert keychain-access-groups.0 -string" +
                                    " '$teamId.$it' ${entitlementFile.canonicalPath}"
                            CommandLineUtils.execute(
                                command = insertKeyChainGroupCMD,
                                workspace = entitlementFile.parentFile,
                                print2Logger = true
                            )
                        }
                    }
                }
            }
        } catch (ignore: Exception) {
            logger.error("插入entitlement文件(${entitlementFile.canonicalPath})的keychain-access-groups失败。", ignore)
            throw ErrorCodeException(
                errorCode = SignMessageCode.ERROR_INSERT_KEYCHAIN_GROUPS,
                defaultMessage = "entitlement插入keychain失败"
            )
        }
    }

    override fun downloadWildcardMobileProvision(mobileProvisionDir: File, ipaSignInfo: IpaSignInfo): File? {
        return null
    }
}
