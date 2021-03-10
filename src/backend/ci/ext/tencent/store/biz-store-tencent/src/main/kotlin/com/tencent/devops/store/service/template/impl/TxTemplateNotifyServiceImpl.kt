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

package com.tencent.devops.store.service.template.impl

import com.tencent.devops.common.client.Client
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.store.dao.template.MarketTemplateDao
import com.tencent.devops.store.pojo.common.TEMPLATE_RELEASE_AUDIT_PASS_TEMPLATE
import com.tencent.devops.store.pojo.common.TEMPLATE_RELEASE_AUDIT_REFUSE_TEMPLATE
import com.tencent.devops.store.pojo.common.enums.AuditTypeEnum
import com.tencent.devops.store.service.template.TemplateNotifyService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.stereotype.Service

@Service
@RefreshScope
class TxTemplateNotifyServiceImpl @Autowired constructor() : TemplateNotifyService {

    @Autowired
    private lateinit var dslContext: DSLContext

    @Autowired
    private lateinit var templateDao: MarketTemplateDao

    @Autowired
    private lateinit var client: Client

    @Value("\${store.templateDetailBaseUrl}")
    private lateinit var templateDetailBaseUrl: String

    private val logger = LoggerFactory.getLogger(TxTemplateNotifyServiceImpl::class.java)

    /**
     * 发送模板发布审核结果通知消息
     * @param templateId 模板ID
     * @param auditType 审核类型
     */
    override fun sendTemplateReleaseAuditNotifyMessage(templateId: String, auditType: AuditTypeEnum) {
        val template = templateDao.getTemplate(dslContext, templateId) ?: return
        val titleParams = mapOf(
            "name" to template.templateName
        )
        val bodyParams = mapOf(
            "name" to template.templateName,
            "version" to template.version,
            "publisher" to template.publisher,
            "nameInBody" to template.templateName,
            "templateStatusMsg" to template.templateStatusMsg,
            "url" to templateDetailBaseUrl + template.templateCode
        )
        val receiver: String = template.creator
        val receivers = mutableSetOf(receiver)
        val templateCode = when (auditType) {
            AuditTypeEnum.AUDIT_SUCCESS -> {
                TEMPLATE_RELEASE_AUDIT_PASS_TEMPLATE
            }
            AuditTypeEnum.AUDIT_REJECT -> {
                TEMPLATE_RELEASE_AUDIT_PASS_TEMPLATE
            }
            else -> {
                TEMPLATE_RELEASE_AUDIT_REFUSE_TEMPLATE
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
