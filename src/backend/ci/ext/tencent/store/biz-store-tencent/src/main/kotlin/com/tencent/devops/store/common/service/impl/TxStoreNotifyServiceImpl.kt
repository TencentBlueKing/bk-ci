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

package com.tencent.devops.store.common.service.impl

import com.tencent.devops.common.api.constant.DEVOPS
import com.tencent.devops.common.api.constant.NAME
import com.tencent.devops.common.api.constant.VERSION
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.store.common.dao.StoreBaseQueryDao
import com.tencent.devops.store.common.dao.StoreMemberDao
import com.tencent.devops.store.common.dao.StoreVersionLogDao
import com.tencent.devops.store.common.service.StoreNotifyService
import com.tencent.devops.store.pojo.common.KEY_PUBLISHER
import com.tencent.devops.store.pojo.common.enums.AuditTypeEnum
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreMemberTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Primary
@Service
class TxStoreNotifyServiceImpl @Autowired constructor() : StoreNotifyService {

    @Autowired
    private lateinit var client: Client

    @Autowired
    private lateinit var dslContext: DSLContext

    @Autowired
    private lateinit var storeBaseQueryDao: StoreBaseQueryDao

    @Autowired
    private lateinit var storeVersionLogDao: StoreVersionLogDao

    @Autowired
    private lateinit var storeMemberDao: StoreMemberDao

    @Value("\${devopsGateway.host:#{null}}")
    private var storeHost: String = ""

    companion object {
        private val logger = LoggerFactory.getLogger(TxStoreNotifyServiceImpl::class.java)
        // 组件发布审核通过消息通知模板
        private const val STORE_RELEASE_AUDIT_PASS_TEMPLATE = "STORE_RELEASE_AUDIT_PASS_TEMPLATE"
        // 组件发布审核被拒消息通知模板
        private const val STORE_RELEASE_AUDIT_REFUSE_TEMPLATE = "STORE_RELEASE_AUDIT_REFUSE_TEMPLATE"
    }

    override fun sendNotifyMessage(
        templateCode: String,
        sender: String,
        receivers: MutableSet<String>,
        titleParams: Map<String, String>?,
        bodyParams: Map<String, String>?,
        cc: MutableSet<String>?,
        bcc: MutableSet<String>?
    ): Result<Boolean> {
        logger.info("sendNotifyMessage params:[$templateCode|$sender|$receivers|$titleParams|$bodyParams|$cc|:$bcc")
        val sendNotifyMessageTemplateRequest = SendNotifyMessageTemplateRequest(
            templateCode = templateCode,
            receivers = receivers,
            titleParams = titleParams,
            bodyParams = bodyParams,
            cc = cc,
            bcc = bcc
        )
        val sendNotifyResult = client.get(ServiceNotifyMessageTemplateResource::class)
            .sendNotifyMessageByTemplate(sendNotifyMessageTemplateRequest)
        logger.info("sendNotifyResult is:$sendNotifyResult")
        return Result(true)
    }

    override fun sendStoreReleaseAuditNotifyMessage(storeId: String, auditType: AuditTypeEnum) {
        val baseRecord = storeBaseQueryDao.getComponentById(dslContext, storeId) ?: return
        val storeVersionLog = storeVersionLogDao.getStoreVersion(dslContext, storeId) ?: return
        val storeCode = baseRecord.storeCode
        val storeType = baseRecord.storeType
        val name = baseRecord.name
        val version = baseRecord.version
        val titleParams = mapOf(
            NAME to name,
            VERSION to version
        )
        val releaseType = storeVersionLog.releaseType
        val storeDetailDir = if (storeType == StoreTypeEnum.IDE_ATOM.type.toByte()) {
            "ide"
        } else {
            StoreTypeEnum.getStoreType(storeType.toInt()).lowercase()
        }
        val host = if (storeType == StoreTypeEnum.DEVX.type.toByte()) {
            storeHost.replace("devops", "developer")
        } else {
            storeHost
        }
        val bodyParams = mapOf(
            NAME to name,
            VERSION to version,
            KEY_PUBLISHER to baseRecord.publisher,
            "releaseType" to if (releaseType != null) I18nUtil.getCodeLanMessage(
                messageCode = "RELEASE_TYPE_" + ReleaseTypeEnum.getReleaseType(releaseType.toInt())
            ) else "",
            "versionDesc" to (storeVersionLog.content ?: ""),
            "nameInBody" to name,
            "statusMsg" to baseRecord.statusMsg,
            "url" to "$host/$storeDetailDir/$storeCode/basicInfo"
        )
        val creator = baseRecord.creator
        val receiver: String = creator
        val ccs = mutableSetOf(creator)
        if (auditType == AuditTypeEnum.AUDIT_SUCCESS) {
            val storeAdminRecords = storeMemberDao.list(
                dslContext = dslContext,
                storeCode = storeCode,
                type = StoreMemberTypeEnum.ADMIN.type.toByte(),
                storeType = storeType
            )
            storeAdminRecords?.map {
                ccs.add(it.username)
            }
        }
        val receivers = mutableSetOf(receiver)
        val templateCode = getNotifyTemplateCode(auditType)
        sendNotifyMessage(
            templateCode = templateCode,
            sender = DEVOPS,
            receivers = receivers,
            titleParams = titleParams,
            bodyParams = bodyParams,
            cc = ccs
        )
    }

    private fun getNotifyTemplateCode(auditType: AuditTypeEnum): String {
        val templateCode = when (auditType) {
            AuditTypeEnum.AUDIT_SUCCESS -> {
                STORE_RELEASE_AUDIT_PASS_TEMPLATE
            }

            AuditTypeEnum.AUDIT_REJECT -> {
                STORE_RELEASE_AUDIT_REFUSE_TEMPLATE
            }

            else -> {
                STORE_RELEASE_AUDIT_REFUSE_TEMPLATE
            }
        }
        return templateCode
    }
}
