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
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.pojo.element.atom.ManualReviewParam
import com.tencent.devops.common.pipeline.pojo.element.atom.ManualReviewParamType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.common.wechatwork.WechatWorkRobotService
import com.tencent.devops.common.wechatwork.WechatWorkService
import com.tencent.devops.notify.constant.NotifyMessageCode.BK_DESIGNATED_APPROVER_APPROVAL
import com.tencent.devops.notify.constant.NotifyMessageCode.BK_LINE_BREAKS_WILL_ESCAPED
import com.tencent.devops.notify.dao.CommonNotifyMessageTemplateDao
import com.tencent.devops.notify.dao.NotifyMessageTemplateDao
import com.tencent.devops.notify.dao.TNotifyMessageTemplateDao
import com.tencent.devops.notify.pojo.NotifyTemplateMessage
import com.tencent.devops.notify.pojo.NotifyTemplateMessageRequest
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.notify.pojo.SubNotifyMessageTemplate
import com.tencent.devops.support.api.service.ServiceMessageApproveResource
import com.tencent.devops.support.model.approval.CompleteMoaWorkItemRequest
import com.tencent.devops.support.model.approval.MoaWorkItemCreateAction
import com.tencent.devops.support.model.approval.MoaWorkItemCreateData
import com.tencent.devops.support.model.approval.MoaWorkItemCreateForm
import com.tencent.devops.support.model.approval.MoaWorkItemCreateKeyAndValue
import com.tencent.devops.support.model.approval.MoaWorkItemCreateUiType
import com.tencent.devops.support.model.approval.MoaWorkItemElement
import com.tencent.devops.support.model.approval.MoaWorkitemCreateCategoryType
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.time.LocalDateTime

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
    private val weworkService: WeworkService,
    private val wechatWorkService: WechatWorkService,
    private val wechatWorkRobotService: WechatWorkRobotService
) : NotifyMessageTemplateServiceImpl(
    dslContext = dslContext,
    notifyMessageTemplateDao = notifyMessageTemplateDao,
    commonNotifyMessageTemplateDao = commonNotifyMessageTemplateDao,
    emailService = emailService,
    rtxService = rtxService,
    wechatService = wechatService,
    weworkService = weworkService,
    wechatWorkService = wechatWorkService,
    wechatWorkRobotService = wechatWorkRobotService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(TXNotifyMessageTemplateServiceImpl::class.java)
    }

    override fun sendOtherSpecialNotifyMessage(
        sendAllNotify: Boolean,
        request: SendNotifyMessageTemplateRequest,
        templateId: String,
        notifyTypeScope: String
    ) {
        if (sendAllNotify || request.notifyType?.contains("MOA") == true) {
            if (!notifyTypeScope.contains("MOA")) {
                logger.warn(
                    "COMMON_NOTIFY_MESSAGE_TEMPLATE_NOT_FOUND|If needed, add on the OP" +
                        "|type=MOA|template=${request.templateCode}"
                )
            } else {
                logger.info("send wework msg: $templateId")
                sendMoaNotifyMessage(request, templateId)
            }
        }
    }

    private fun sendMoaNotifyMessage(
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
                        name = checkParamName(param.chineseName) ?: param.key,
                        uiType = when (param.valueType) {
                            ManualReviewParamType.STRING -> MoaWorkItemCreateUiType.TEXT_BOX.value
                            ManualReviewParamType.TEXTAREA -> MoaWorkItemCreateUiType.TEXT_BOX.value
                            ManualReviewParamType.BOOLEAN -> MoaWorkItemCreateUiType.RADIO_BOX.value
                            ManualReviewParamType.ENUM -> MoaWorkItemCreateUiType.DROP_DOWN_LIST.value
                            ManualReviewParamType.MULTIPLE -> MoaWorkItemCreateUiType.CHECK_BOX.value
                        },
                        values = when (param.valueType) {
                            ManualReviewParamType.STRING -> listOf(param.value.toString())
                            ManualReviewParamType.TEXTAREA -> listOf(param.value.toString())
                            ManualReviewParamType.BOOLEAN -> listOf("true", "false")
                            ManualReviewParamType.ENUM, ManualReviewParamType.MULTIPLE -> param.options?.map { it.key }
                        }
                    ).apply {
                        if (param.valueType == ManualReviewParamType.STRING) {
                            description += I18nUtil.getCodeLanMessage(
                                messageCode = BK_LINE_BREAKS_WILL_ESCAPED
                            )
                        }
                    }
                }
            } catch (ignore: Throwable) {
                null
            }
        }
        val detailView = JsonUtil.toOrNull(
            replaceContentParams(request.bodyParams, moaTplRecord.body),
            object : TypeReference<List<MoaWorkItemCreateKeyAndValue>>() {}
        )

        // 保持和complete时的id一致, 考虑不需要持久化该字段，采用可计算出的 signature 前32位作为id
        val processInstId = request.callbackData?.get("signature")?.take(32) ?: UUIDUtil.generate()
        val moaWorkItemElementList = request.receivers.map { receiver ->
            MoaWorkItemElement(
                actions = listOf(
                    MoaWorkItemCreateAction(
                        displayName = "同意",
                        value = "agree"
                    ),
                    MoaWorkItemCreateAction(
                        displayName = "驳回",
                        value = "reject"
                    )
                ),
                detailView = detailView,
                activity = I18nUtil.getCodeLanMessage(
                    messageCode = BK_DESIGNATED_APPROVER_APPROVAL
                ),
                category = MoaWorkitemCreateCategoryType.IT.id,
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

    override fun completeNotifyMessageByTemplate(request: SendNotifyMessageTemplateRequest): Result<Boolean> {
        val templateCode = request.templateCode
        // 查出消息模板
        val commonNotifyMessageTemplateRecord =
            commonNotifyMessageTemplateDao.getCommonNotifyMessageTemplateByCode(dslContext, templateCode)
                ?: return MessageUtil.generateResponseDataObject(
                    messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                    params = arrayOf(templateCode),
                    data = false,
                    language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                )
        // 暂时仅支持moa
        val moaTplRecord = tNotifyMessageTemplateDao.getMoaNotifyMessageTemplate(
            dslContext = dslContext,
            commonTemplateId = commonNotifyMessageTemplateRecord.id
        )!!
        // 保持和send时的id一致, 考虑不需要持久化该字段，采用可计算出的 signature 前32位作为id
        val processInstId = request.callbackData?.get("signature")?.take(32) ?: return Result(false)
        request.receivers.map { receiver ->
            client.get(ServiceMessageApproveResource::class).createMoaWorkItemMessageComplete(
                CompleteMoaWorkItemRequest(
                    activity = I18nUtil.getCodeLanMessage(
                        messageCode = BK_DESIGNATED_APPROVER_APPROVAL
                    ),
                    category = MoaWorkitemCreateCategoryType.IT.id,
                    handler = receiver,
                    processInstId = processInstId,
                    processName = moaTplRecord.processName
                )
            )
        }
        return Result(true)
    }

    override fun getOtherNotifyMessageTemplate(
        subTemplateList: MutableList<SubNotifyMessageTemplate>,
        templateId: String
    ) {
        val moa = tNotifyMessageTemplateDao.getMoaNotifyMessageTemplate(dslContext, templateId)
        if (null != moa) {
            subTemplateList.add(
                SubNotifyMessageTemplate(
                    notifyTypeScope = listOf("MOA"),
                    title = moa.title,
                    body = moa.body,
                    creator = moa.creator,
                    modifier = moa.modifior,
                    callBackUrl = moa.callbackUrl,
                    processName = moa.processName,
                    createTime = (moa.createTime as LocalDateTime).timestampmilli(),
                    updateTime = (moa.updateTime as LocalDateTime).timestampmilli()
                )
            )
        }
    }

    override fun updateOtherNotifyMessageTemplate(
        notifyMessageTemplateRequest: NotifyTemplateMessageRequest,
        notifyTypeScopeSet: MutableSet<String>
    ): Boolean {
        var hasMoa = false
        // 判断提交的数据中是否存在同样类型的
        notifyMessageTemplateRequest.msg.forEach {
            if (it.notifyTypeScope.contains("MOA") && !hasMoa) {
                hasMoa = true
                notifyTypeScopeSet.add("MOA")
            } else if (it.notifyTypeScope.contains("MOA") && hasMoa) {
                return false
            }
        }
        return true
    }

    override fun updateOtherSpecialTemplate(
        it: NotifyTemplateMessage,
        templateId: String,
        uid: String,
        userId: String
    ) {
        if (it.notifyTypeScope.contains("MOA")) {
            updateMoaNotifyMessageTemplate(
                id = templateId,
                newId = uid,
                userId = userId,
                addNotifyTemplateMessage = it
            )
        }
    }

    private fun updateMoaNotifyMessageTemplate(
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

    private fun checkParamName(name: String?) = if (name.isNullOrBlank()) null else name
}
