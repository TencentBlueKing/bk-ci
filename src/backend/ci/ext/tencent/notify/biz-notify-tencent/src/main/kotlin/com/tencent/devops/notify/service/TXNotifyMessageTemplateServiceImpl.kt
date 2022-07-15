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
package com.tencent.devops.notify.service

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.pojo.element.atom.ManualReviewParam
import com.tencent.devops.common.pipeline.pojo.element.atom.ManualReviewParamType
import com.tencent.devops.notify.dao.CommonNotifyMessageTemplateDao
import com.tencent.devops.notify.dao.NotifyMessageTemplateDao
import com.tencent.devops.notify.dao.TNotifyMessageTemplateDao
import com.tencent.devops.notify.pojo.NotifyTemplateMessage
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.support.api.service.ServiceMessageApproveResource
import com.tencent.devops.support.model.approval.MoaWorkItemCreateAction
import com.tencent.devops.support.model.approval.MoaWorkItemCreateData
import com.tencent.devops.support.model.approval.MoaWorkItemCreateForm
import com.tencent.devops.support.model.approval.MoaWorkItemCreateUiType
import com.tencent.devops.support.model.approval.MoaWorkItemElement
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Primary
@Service
@Suppress("ALL")
class TXNotifyMessageTemplateServiceImpl @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val tNotifyMessageTemplateDao: TNotifyMessageTemplateDao,
    private val notifyMessageTemplateDao: NotifyMessageTemplateDao,
    private val commonNotifyMessageTemplateDao: CommonNotifyMessageTemplateDao,
    private val emailService: EmailService,
    private val rtxService: RtxService,
    private val wechatService: WechatService,
    private val weworkService: WeworkService
) : NotifyMessageTemplateServiceImpl(
    dslContext = dslContext,
    notifyMessageTemplateDao = notifyMessageTemplateDao,
    commonNotifyMessageTemplateDao = commonNotifyMessageTemplateDao,
    emailService = emailService,
    rtxService = rtxService,
    wechatService = wechatService,
    weworkService = weworkService
) {

    private val logger = LoggerFactory.getLogger(TXNotifyMessageTemplateServiceImpl::class.java)

    override fun sendMoaNotifyMessage(
        request: SendNotifyMessageTemplateRequest,
        commonTemplateId: String
    ) {
        val moaTplRecord = tNotifyMessageTemplateDao.getMoaNotifyMessageTemplate(
            dslContext = dslContext,
            commonTemplateId = commonTemplateId
        )!!
        val moaForm = request.bodyParams?.getOrDefault("manualReviewParam", "{}")?.let {
            try {
                JsonUtil.to(it, object : TypeReference<List<ManualReviewParam>?>() {})?.map { param ->
                    MoaWorkItemCreateForm(
                        defaultValue = param.value.toString(),
                        description = param.desc,
                        isRequired = param.required,
                        name = param.chineseName ?: param.key,
                        uiType = when (param.valueType) {
                            ManualReviewParamType.STRING -> MoaWorkItemCreateUiType.TextBox.value
                            ManualReviewParamType.TEXTAREA -> MoaWorkItemCreateUiType.TextBox.value
                            ManualReviewParamType.BOOLEAN -> MoaWorkItemCreateUiType.RadioBox.value
                            ManualReviewParamType.ENUM -> MoaWorkItemCreateUiType.DropDownList.value
                            ManualReviewParamType.MULTIPLE -> MoaWorkItemCreateUiType.CheckBox.value
                        },
                        values = when (param.valueType) {
                            ManualReviewParamType.STRING -> listOf(param.value.toString())
                            ManualReviewParamType.TEXTAREA -> listOf(param.value.toString())
                            ManualReviewParamType.BOOLEAN -> listOf("true", "false")
                            ManualReviewParamType.ENUM, ManualReviewParamType.MULTIPLE -> param.options?.map { it.key }
                        }
                    )
                }
            } catch (ignore: Throwable) {
                null
            }
        }
        val processInstId = UUIDUtil.generate()
        val moaWorkItemElementList = request.receivers.map { receiver ->
            MoaWorkItemElement(
                actions = listOf(
                    MoaWorkItemCreateAction(
                        displayName = "同意",
                        value = "agree"
                    ), MoaWorkItemCreateAction(
                        displayName = "驳回",
                        value = "reject"
                    )
                ),
                activity = "指定审批人审批",
                callbackUrl = moaTplRecord.callbackUrl,
                form = moaForm,
                formUrl = request.bodyParams?.get("reviewUrl"),
                mobileFormUrl = request.bodyParams?.get("reviewAppUrl"),
                handler = receiver,
                processInstId = processInstId,
                processName = moaTplRecord.processName,
                title = replaceContentParams(request.titleParams, moaTplRecord.title),
                data = request.callbackData?.map { MoaWorkItemCreateData(it.key, listOf(it.value)) } ?: emptyList()
            )
        }
        val createMoaMessageApprovalResult = client.get(ServiceMessageApproveResource::class)
            .createMoaWorkItemMessageApproval(moaWorkItemElementList)
        logger.info("createMoaMessageApprovalResult is :$createMoaMessageApprovalResult")
    }

    override fun updateMoaNotifyMessageTemplate(
        id: String,
        newId: String,
        userId: String,
        addNotifyTemplateMessage: NotifyTemplateMessage
    ) {
        tNotifyMessageTemplateDao.saveOrUpdateMoaNotifyMessageTemplate(
            dslContext = dslContext,
            id = id,
            newId = newId,
            userId = userId,
            addNotifyTemplateMessage = addNotifyTemplateMessage
        )
    }
}
