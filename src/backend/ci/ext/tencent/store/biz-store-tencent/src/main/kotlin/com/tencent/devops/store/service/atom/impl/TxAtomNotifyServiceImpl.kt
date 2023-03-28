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

package com.tencent.devops.store.service.atom.impl

import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.store.dao.atom.AtomDao
import com.tencent.devops.store.dao.atom.MarketAtomVersionLogDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.pojo.common.ATOM_RELEASE_AUDIT_PASS_TEMPLATE
import com.tencent.devops.store.pojo.common.ATOM_RELEASE_AUDIT_REFUSE_TEMPLATE
import com.tencent.devops.store.pojo.common.enums.AuditTypeEnum
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreMemberTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.atom.AtomNotifyService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class TxAtomNotifyServiceImpl @Autowired constructor() : AtomNotifyService {

    private val logger = LoggerFactory.getLogger(TxAtomNotifyServiceImpl::class.java)

    @Autowired
    private lateinit var dslContext: DSLContext

    @Autowired
    private lateinit var atomDao: AtomDao

    @Autowired
    private lateinit var atomVersionLogDao: MarketAtomVersionLogDao

    @Autowired
    private lateinit var storeMemberDao: StoreMemberDao

    @Autowired
    private lateinit var client: Client

    @Value("\${store.atomDetailBaseUrl}")
    private lateinit var atomDetailBaseUrl: String

    /**
     * 发送插件发布审核结果通知消息
     * @param atomId 插件ID
     * @param auditType 审核类型
     */
    override fun sendAtomReleaseAuditNotifyMessage(atomId: String, auditType: AuditTypeEnum) {
        val atom = atomDao.getPipelineAtom(dslContext, atomId) ?: return
        // 查出版本日志
        val atomVersionLog = atomVersionLogDao.getAtomVersion(dslContext, atomId)
        val atomCode = atom.atomCode
        val atomName = atom.name
        val titleParams = mapOf(
            "name" to atomName,
            "version" to atom.version
        )
        val releaseType = atomVersionLog.releaseType
        val bodyParams = mapOf(
            "name" to atomName,
            "version" to atom.version,
            "publisher" to atom.publisher,
            "releaseType" to if (releaseType != null) MessageUtil.getCodeLanMessage(
                messageCode = "RELEASE_TYPE_" + ReleaseTypeEnum.getReleaseType(releaseType.toInt()),
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            ) else "",
            "versionDesc" to (atomVersionLog.content ?: ""),
            "nameInBody" to atomName,
            "atomStatusMsg" to atom.atomStatusMsg,
            "url" to atomDetailBaseUrl + atomCode
        )
        val creator = atom.creator
        val receiver: String = creator
        val ccs = mutableSetOf(creator)
        if (auditType == AuditTypeEnum.AUDIT_SUCCESS) {
            val atomAdminRecords = storeMemberDao.list(
                dslContext = dslContext,
                storeCode = atomCode,
                type = StoreMemberTypeEnum.ADMIN.type.toByte(),
                storeType = StoreTypeEnum.ATOM.type.toByte()
            )
            atomAdminRecords?.map {
                ccs.add(it.username)
            }
        }
        val receivers = mutableSetOf(receiver)
        val templateCode = when (auditType) {
            AuditTypeEnum.AUDIT_SUCCESS -> {
                ATOM_RELEASE_AUDIT_PASS_TEMPLATE
            }
            AuditTypeEnum.AUDIT_REJECT -> {
                ATOM_RELEASE_AUDIT_REFUSE_TEMPLATE
            }
            else -> {
                ATOM_RELEASE_AUDIT_REFUSE_TEMPLATE
            }
        }
        val sendNotifyMessageTemplateRequest = SendNotifyMessageTemplateRequest(
            templateCode = templateCode,
            receivers = receivers,
            cc = receivers,
            titleParams = titleParams,
            bodyParams = bodyParams
        )
        val sendNotifyResult = client.get(ServiceNotifyMessageTemplateResource::class)
            .sendNotifyMessageByTemplate(sendNotifyMessageTemplateRequest)
        logger.info("sendNotifyResult is:$sendNotifyResult")
    }
}
