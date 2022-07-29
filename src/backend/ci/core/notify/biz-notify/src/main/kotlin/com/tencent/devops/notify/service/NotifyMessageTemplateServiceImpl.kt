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

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.notify.enums.EnumEmailFormat
import com.tencent.devops.common.notify.enums.EnumEmailType
import com.tencent.devops.common.notify.enums.EnumNotifyPriority
import com.tencent.devops.common.notify.enums.EnumNotifySource
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.model.notify.tables.records.TCommonNotifyMessageTemplateRecord
import com.tencent.devops.notify.dao.CommonNotifyMessageTemplateDao
import com.tencent.devops.notify.dao.NotifyMessageTemplateDao
import com.tencent.devops.notify.model.WeworkNotifyMessageWithOperation
import com.tencent.devops.notify.pojo.EmailNotifyMessage
import com.tencent.devops.notify.pojo.NotifyContext
import com.tencent.devops.notify.pojo.NotifyMessageCommonTemplate
import com.tencent.devops.notify.pojo.NotifyMessageContextRequest
import com.tencent.devops.notify.pojo.NotifyTemplateMessage
import com.tencent.devops.notify.pojo.NotifyTemplateMessageRequest
import com.tencent.devops.notify.pojo.RtxNotifyMessage
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.notify.pojo.SubNotifyMessageTemplate
import com.tencent.devops.notify.pojo.WechatNotifyMessage
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
@Suppress("ALL")
class NotifyMessageTemplateServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val notifyMessageTemplateDao: NotifyMessageTemplateDao,
    private val commonNotifyMessageTemplateDao: CommonNotifyMessageTemplateDao,
    private val emailService: EmailService,
    private val rtxService: RtxService,
    private val wechatService: WechatService,
    private val weworkService: WeworkService
) : NotifyMessageTemplateService {

    private val logger = LoggerFactory.getLogger(NotifyMessageTemplateServiceImpl::class.java)

    @Value("\${wework.domain}")
    private val userUseDomain: Boolean? = true

    /**
     * 根据查找到的消息通知模板主体信息来获取具体信息
     * @param userId
     * @param templateId 对应模板通知模板的ID，在消息通知模板表中
     */
    override fun getNotifyMessageTemplates(userId: String, templateId: String): Result<Page<SubNotifyMessageTemplate>> {
        val email = notifyMessageTemplateDao.getEmailNotifyMessageTemplate(dslContext, templateId)
        val wechat = notifyMessageTemplateDao.getWechatNotifyMessageTemplate(dslContext, templateId)
        val rtx = notifyMessageTemplateDao.getRtxNotifyMessageTemplate(dslContext, templateId)
        val subTemplateList = mutableListOf<SubNotifyMessageTemplate>()
        if (null != email) {
            subTemplateList.add(
                SubNotifyMessageTemplate(
                    notifyTypeScope = listOf(NotifyType.EMAIL.name),
                    title = email.title,
                    body = email.body,
                    bodyFormat = (email.bodyFormat as Byte).toInt(),
                    emailType = (email.emailType as Byte).toInt(),
                    creator = email.creator,
                    modifier = email.modifior,
                    createTime = (email.createTime as LocalDateTime).timestampmilli(),
                    updateTime = (email.updateTime as LocalDateTime).timestampmilli()
                )
            )
        }
        if (null != rtx) {
            subTemplateList.add(
                SubNotifyMessageTemplate(
                    notifyTypeScope = listOf(NotifyType.RTX.name),
                    title = rtx.title,
                    body = rtx.body,
                    creator = rtx.creator,
                    modifier = rtx.modifior,
                    createTime = (rtx.createTime as LocalDateTime).timestampmilli(),
                    updateTime = (rtx.updateTime as LocalDateTime).timestampmilli()
                )
            )
        }
        if (null != wechat) {
            subTemplateList.add(
                SubNotifyMessageTemplate(
                    notifyTypeScope = listOf(NotifyType.WECHAT.name),
                    title = wechat.title,
                    body = wechat.body,
                    creator = wechat.creator,
                    modifier = wechat.modifior,
                    createTime = (wechat.createTime as LocalDateTime).timestampmilli(),
                    updateTime = (wechat.updateTime as LocalDateTime).timestampmilli()
                )
            )
        }

        return Result(
            // 最多三条内容
            Page(
                count = subTemplateList.size.toLong(),
                page = PageUtil.DEFAULT_PAGE,
                pageSize = PageUtil.DEFAULT_PAGE_SIZE,
                totalPages = 1,
                records = subTemplateList
            )
        )
    }

    /**
     * 搜索消息模板的公共信息
     * @param userId 用户ID
     * @param templateCode 模板代码
     * @param templateName 模板名称
     * @param page 页数
     * @param pageSize 每页记录条数
     */
    override fun getCommonNotifyMessageTemplates(
        userId: String,
        templateCode: String?,
        templateName: String?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<NotifyMessageCommonTemplate>> {
        val validPage = PageUtil.getValidPage(page)
        val validPageSize = pageSize ?: -1
        val commonTemplates = notifyMessageTemplateDao.searchCommonNotifyMessageTemplates(
            dslContext = dslContext,
            templateCode = templateCode?.trim(),
            templateName = templateName?.trim(),
            page = validPage,
            pageSize = validPageSize,
            new = true
        )
        val commonTemplateList = mutableListOf<NotifyMessageCommonTemplate>()
        commonTemplates?.forEach {
            commonTemplateList.add(it.generateCommonNotifyMessageTemplate())
        }
        val count = commonTemplateList.size.toLong()
        val totalPages = PageUtil.calTotalPage(pageSize, count)
        return Result(
            Page(
                count = count,
                page = validPage,
                pageSize = validPageSize,
                totalPages = totalPages,
                records = commonTemplateList
            )
        )
    }

    private fun TCommonNotifyMessageTemplateRecord.generateCommonNotifyMessageTemplate(): NotifyMessageCommonTemplate {
        return NotifyMessageCommonTemplate(
            id = id,
            templateCode = templateCode,
            templateName = templateName,
            priority = priority.toString(),
            source = source.toInt()
        )
    }

    /**
     * 添加消息通知模板信息
     * @param userId 执行人
     * @param addNotifyMessageTemplateRequest 添加消息模板信息
     */
    @Suppress("UNCHECKED_CAST")
    override fun addNotifyMessageTemplate(
        userId: String,
        addNotifyMessageTemplateRequest: NotifyTemplateMessageRequest
    ): Result<Boolean> {
        // 每次最多添加三条消息，一条EMAIL，一条RTX、一条WECHAT
        if (addNotifyMessageTemplateRequest.msg.size > 3) {
            return MessageCodeUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf("notifyTypeNum"),
                data = false
            )
        }

        // 获取本次添加数据的消息模板类型
        val notifyTypeScopeSet = mutableSetOf<String>()
        addNotifyMessageTemplateRequest.msg.forEach {
            if (it.notifyTypeScope.contains(NotifyType.RTX.name)) {
                notifyTypeScopeSet.add(NotifyType.RTX.name)
            }
            if (it.notifyTypeScope.contains(NotifyType.WECHAT.name)) {
                notifyTypeScopeSet.add(NotifyType.WECHAT.name)
            }
            if (it.notifyTypeScope.contains(NotifyType.EMAIL.name)) {
                if (it.emailType == null || it.bodyFormat == null) {
                    return MessageCodeUtil.generateResponseDataObject(
                        messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                        params = arrayOf("${it.bodyFormat} or ${it.emailType}"),
                        data = false
                    )
                }
                notifyTypeScopeSet.add(NotifyType.EMAIL.name)
            }
        }

        // 添加的信息中的模板类型是非法数据
        if (notifyTypeScopeSet.size == 0) {
            return MessageCodeUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf("notifyType"),
                data = false
            )
        }

        // 已存在判断
        val commonTplByCode = notifyMessageTemplateDao.getCommonNotifyMessageTemplateByCode(
            dslContext = dslContext,
            templateCode = addNotifyMessageTemplateRequest.templateCode
        )
        val commonTplByName = notifyMessageTemplateDao.getCommonNotifyMessageTemplateByName(
            dslContext = dslContext,
            templateName = addNotifyMessageTemplateRequest.templateName
        )
        if (null != commonTplByCode || null != commonTplByName) {
            return MessageCodeUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf("Code/Name"),
                data = false
            )
        }

        // 插入消息模板
        val id = UUIDUtil.generate()
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            // 插入公共消息模板
            notifyMessageTemplateDao.addCommonNotifyMessageTemplate(
                dslContext = context,
                id = id,
                addNotifyTemplateMessageRequest = addNotifyMessageTemplateRequest,
                notifyTypeScope = notifyTypeScopeSet
            )
            addNotifyMessageTemplateRequest.msg.forEach {
                if (it.notifyTypeScope.contains(NotifyType.EMAIL.name)) {
                    notifyMessageTemplateDao.addEmailsNotifyMessageTemplate(
                        dslContext = context,
                        id = id,
                        newId = UUIDUtil.generate(),
                        userId = userId,
                        addNotifyTemplateMessage = it
                    )
                }
                if (it.notifyTypeScope.contains(NotifyType.RTX.name)) {
                    notifyMessageTemplateDao.addRTXNotifyMessageTemplate(
                        dslContext = context,
                        id = id,
                        newId = UUIDUtil.generate(),
                        userId = userId,
                        notifyTemplateMessage = it
                    )
                }
                if (it.notifyTypeScope.contains(NotifyType.WECHAT.name)) {
                    notifyMessageTemplateDao.addWECHATNotifyMessageTemplate(
                        dslContext = context,
                        id = id,
                        newId = UUIDUtil.generate(),
                        userId = userId,
                        notifyTemplateMessage = it
                    )
                }
            }
        }
        return Result(true)
    }

    /**
     * 更新消息通知模板信息
     * @param userId 用户ID
     * @param templateId 模板ID
     * @param notifyMessageTemplateRequest 消息模板更新内容
     */
    override fun updateNotifyMessageTemplate(
        userId: String,
        templateId: String,
        notifyMessageTemplateRequest: NotifyTemplateMessageRequest
    ): Result<Boolean> {
        if (notifyMessageTemplateRequest.msg.size > 3) {
            return MessageCodeUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf("TplNum"),
                data = false
            )
        }
        var hasEmail = false
        var hasRtx = false
        var hasWechat = false
        val notifyTypeScopeSet = mutableSetOf<String>()
        // 判断提交的数据中是否存在同样类型的
        notifyMessageTemplateRequest.msg.forEach {
            if (it.notifyTypeScope.contains(NotifyType.EMAIL.name) && !hasEmail) {
                hasEmail = true
                notifyTypeScopeSet.add(NotifyType.EMAIL.name)
            } else if (it.notifyTypeScope.contains(NotifyType.EMAIL.name) && hasEmail) {
                return MessageCodeUtil.generateResponseDataObject(
                    messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                    params = arrayOf("notifyType"),
                    data = false
                )
            }

            if (it.notifyTypeScope.contains(NotifyType.RTX.name) && !hasRtx) {
                hasRtx = true
                notifyTypeScopeSet.add(NotifyType.RTX.name)
            } else if (it.notifyTypeScope.contains(NotifyType.RTX.name) && hasRtx) {
                return MessageCodeUtil.generateResponseDataObject(
                    messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                    params = arrayOf("notifyType"),
                    data = false
                )
            }

            if (it.notifyTypeScope.contains(NotifyType.WECHAT.name) && !hasWechat) {
                hasWechat = true
                notifyTypeScopeSet.add(NotifyType.WECHAT.name)
            } else if (it.notifyTypeScope.contains(NotifyType.WECHAT.name) && hasWechat) {
                return MessageCodeUtil.generateResponseDataObject(
                    messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                    params = arrayOf("notifyType"),
                    data = false
                )
            }
        }

        if (!updateOtherNotifyMessageTemplate(notifyMessageTemplateRequest, notifyTypeScopeSet)) {
            return MessageCodeUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf("notifyType"),
                data = false
            )
        }

        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            // 更新Common表中的主体信息
            notifyMessageTemplateDao.updateCommonNotifyMessageTemplate(
                dslContext = context,
                templateId = templateId,
                notifyMessageTemplateRequest = notifyMessageTemplateRequest,
                notifyTypeScopeSet = notifyTypeScopeSet
            )
            val uid = UUIDUtil.generate()
            // 根据模板类型向消息模板信息表中添加信息
            notifyMessageTemplateRequest.msg.forEach {
                if (it.notifyTypeScope.contains(NotifyType.WECHAT.name)) {
                    val num = notifyMessageTemplateDao.countWechatMessageTemplate(dslContext, templateId)
                    if (num > 0) {
                        notifyMessageTemplateDao.updateWechatNotifyMessageTemplate(
                            dslContext = dslContext,
                            userId = userId,
                            templateId = templateId,
                            notifyMessageTemplate = it
                        )
                    } else {
                        notifyMessageTemplateDao.addWECHATNotifyMessageTemplate(
                            dslContext = dslContext,
                            id = templateId,
                            newId = uid,
                            userId = userId,
                            notifyTemplateMessage = it
                        )
                    }
                }
                if (it.notifyTypeScope.contains(NotifyType.RTX.name)) {
                    val num = notifyMessageTemplateDao.countRtxMessageTemplate(dslContext, templateId)
                    if (num > 0) {
                        notifyMessageTemplateDao.updateRtxNotifyMessageTemplate(
                            dslContext = dslContext,
                            userId = userId,
                            templateId = templateId,
                            notifyMessageTemplate = it
                        )
                    } else {
                        notifyMessageTemplateDao.addRTXNotifyMessageTemplate(
                            dslContext = dslContext,
                            id = templateId,
                            newId = uid,
                            userId = userId,
                            notifyTemplateMessage = it
                        )
                    }
                }
                if (it.notifyTypeScope.contains(NotifyType.EMAIL.name)) {
                    val num = notifyMessageTemplateDao.countEmailMessageTemplate(dslContext, templateId)
                    if (num > 0) {
                        notifyMessageTemplateDao.updateEmailsNotifyMessageTemplate(
                            dslContext = dslContext,
                            userId = userId,
                            templateId = templateId,
                            notifyTemplateMessage = it
                        )
                    } else {
                        notifyMessageTemplateDao.addEmailsNotifyMessageTemplate(
                            dslContext = dslContext,
                            id = templateId,
                            newId = uid,
                            userId = userId,
                            addNotifyTemplateMessage = it
                        )
                    }
                }
                updateOtherSpecialTemplate(it, templateId, uid, userId)
            }
        }
        return Result(true)
    }

    fun updateOtherNotifyMessageTemplate(
        notifyMessageTemplateRequest: NotifyTemplateMessageRequest,
        notifyTypeScopeSet: MutableSet<String>
    ): Boolean {
        return true
    }

    fun updateOtherSpecialTemplate(
        it: NotifyTemplateMessage,
        templateId: String,
        uid: String,
        userId: String
    ) {
    }

    /**
     * 删除消息模板
     * @param templateId 消息模板ID
     * @param notifyType 要删除的消息模板类型
     */
    @Suppress("UNCHECKED_CAST")
    override fun deleteNotifyMessageTemplate(templateId: String, notifyType: String): Result<Boolean> {
        logger.info("deleteBaseNotifyMessageTemplate templateId is :$templateId")
        dslContext.transaction { t ->
            val context = DSL.using(t)
            val record = notifyMessageTemplateDao.getCommonNotifyMessageTemplatesNotifyType(context, templateId)
            logger.info("获取消息类型：${record?.get("NOTIFY_TYPE_SCOPE") as String}")
            val notifyTypeStr = record["NOTIFY_TYPE_SCOPE"] as String
            val existsNotifyType =
                JsonUtil.getObjectMapper().readValue(notifyTypeStr, List::class.java) as ArrayList<String>
            logger.info("删除消息模板子表信息：$notifyType ${NotifyType.EMAIL} ${notifyType == NotifyType.EMAIL.name}")
            when (notifyType) {
                NotifyType.EMAIL.name -> {
                    notifyMessageTemplateDao.deleteEmailsNotifyMessageTemplate(context, templateId)
                }
                NotifyType.RTX.name -> {
                    notifyMessageTemplateDao.deleteRtxNotifyMessageTemplate(context, templateId)
                }
                NotifyType.WECHAT.name -> {
                    notifyMessageTemplateDao.deleteWechatNotifyMessageTemplate(context, templateId)
                }
            }

            if (existsNotifyType.size == 1 && existsNotifyType[0] == notifyType) {
                logger.info("删除Common表信息")
                notifyMessageTemplateDao.deleteCommonNotifyMessageTemplate(context, templateId)
                return@transaction
            }
            logger.info("修改Common表信息")
            existsNotifyType.remove(notifyType)
            notifyMessageTemplateDao.modifyNotifyTypeScope(context, existsNotifyType, templateId)
        }
        return Result(true)
    }

    /**
     * 删除消息模板主表信息
     * @param templateId 消息模板ID
     */
    override fun deleteCommonNotifyMessageTemplate(templateId: String): Result<Boolean> {
        notifyMessageTemplateDao.deleteCommonNotifyMessageTemplate(dslContext, templateId)
        notifyMessageTemplateDao.deleteEmailsNotifyMessageTemplate(dslContext, templateId)
        notifyMessageTemplateDao.deleteRtxNotifyMessageTemplate(dslContext, templateId)
        notifyMessageTemplateDao.deleteWechatNotifyMessageTemplate(dslContext, templateId)
        return Result(true)
    }

    override fun sendNotifyMessageByTemplate(request: SendNotifyMessageTemplateRequest): Result<Boolean> {
        val templateCode = request.templateCode
        // 查出消息模板
        val commonNotifyMessageTemplateRecord =
            commonNotifyMessageTemplateDao.getCommonNotifyMessageTemplateByCode(dslContext, templateCode)
                ?: return MessageCodeUtil.generateResponseDataObject(
                    messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                    params = arrayOf(templateCode),
                    data = false
                )

        val sendAllNotify = request.notifyType == null
        val notifyTypeScope = commonNotifyMessageTemplateRecord.notifyTypeScope

        // 邮件消息
        if (sendAllNotify || request.notifyType?.contains(NotifyType.EMAIL.name) == true) {
            if (!notifyTypeScope.contains(NotifyType.EMAIL.name)) {
                logger.warn(
                    "COMMON_NOTIFY_MESSAGE_TEMPLATE_NOT_FOUND|If needed, add on the OP" +
                        "|type=${NotifyType.EMAIL}|template=${request.templateCode}"
                )
            } else {
                val emailTplRecord = notifyMessageTemplateDao.getEmailNotifyMessageTemplate(
                    dslContext,
                    commonNotifyMessageTemplateRecord.id
                )!!
                // 替换标题里的动态参数
                val title = replaceContentParams(request.titleParams, emailTplRecord.title)
                // 替换内容里的动态参数
                val body = replaceContentParams(request.bodyParams, emailTplRecord.body)
                sendEmailNotifyMessage(
                    commonNotifyMessageTemplate = commonNotifyMessageTemplateRecord,
                    sendNotifyMessageTemplateRequest = request,
                    title = title,
                    body = body,
                    sender = emailTplRecord.sender
                )
            }
        }

        // 企业微信消息
        if (sendAllNotify || request.notifyType?.contains(NotifyType.RTX.name) == true) {
            if (!notifyTypeScope.contains(NotifyType.RTX.name)) {
                logger.warn(
                    "COMMON_NOTIFY_MESSAGE_TEMPLATE_NOT_FOUND|If needed, add on the OP" +
                        "|type=${NotifyType.RTX}|template=${request.templateCode}"
                )
            } else {
                logger.info("send wework msg: ${commonNotifyMessageTemplateRecord.id}")
                val weworkTplRecord =
                    notifyMessageTemplateDao.getRtxNotifyMessageTemplate(
                        dslContext = dslContext,
                        commonTemplateId = commonNotifyMessageTemplateRecord.id
                    )!!
                val title = replaceContentParams(request.titleParams, weworkTplRecord.title)
                // 替换内容里的动态参数
                val body = replaceContentParams(request.bodyParams, weworkTplRecord.body)
                logger.info("send wework msg: $body ${weworkTplRecord.sender}")
                sendWeworkNotifyMessage(
                    commonNotifyMessageTemplate = commonNotifyMessageTemplateRecord,
                    sendNotifyMessageTemplateRequest = request,
                    body = "$title" + "\n\n" + "$body",
                    sender = weworkTplRecord.sender
                )
            }
        }

        // 微信消息
        if (sendAllNotify || request.notifyType?.contains(NotifyType.WECHAT.name) == true) {
            if (!notifyTypeScope.contains(NotifyType.WECHAT.name)) {
                logger.warn(
                    "COMMON_NOTIFY_MESSAGE_TEMPLATE_NOT_FOUND|If needed, add on the OP" +
                        "|type=${NotifyType.WECHAT}|template=${request.templateCode}"
                )
            } else {
                val wechatTplRecord = notifyMessageTemplateDao.getWechatNotifyMessageTemplate(
                    dslContext = dslContext,
                    commonTemplateId = commonNotifyMessageTemplateRecord.id
                )!!
                // 替换内容里的动态参数
                val body = replaceContentParams(request.bodyParams, wechatTplRecord.body)
                sendWechatNotifyMessage(
                    commonNotifyMessageTemplate = commonNotifyMessageTemplateRecord,
                    sendNotifyMessageTemplateRequest = request,
                    body = body,
                    sender = wechatTplRecord.sender
                )
            }
        }

        // 新企业微信实现
        if (sendAllNotify || request.notifyType?.contains(NotifyType.WEWORK.name) == true) {
            if (!notifyTypeScope.contains(NotifyType.WEWORK.name)) {
                logger.warn(
                    "COMMON_NOTIFY_MESSAGE_TEMPLATE_NOT_FOUND|If needed, add on the OP" +
                        "|type=${NotifyType.WEWORK}|template=${request.templateCode}"
                )
            } else {
                val weworkTplRecord = notifyMessageTemplateDao.getWeworkNotifyMessageTemplate(
                    dslContext = dslContext,
                    commonTemplateId = commonNotifyMessageTemplateRecord.id
                )!!
                // 替换内容里的动态参数
                val title = replaceContentParams(request.titleParams, weworkTplRecord.title)
                val body = replaceContentParams(request.bodyParams, weworkTplRecord.body)
                sendWeworkNotifyMessage(
                    commonNotifyMessageTemplate = commonNotifyMessageTemplateRecord,
                    sendNotifyMessageTemplateRequest = request,
                    body = "$title" + "\n\n" + "$body",
                    sender = weworkTplRecord.sender
                )
            }
        }
        // 其余内部实现
        sendOtherSpecialNotifyMessage(sendAllNotify, request, commonNotifyMessageTemplateRecord.id, notifyTypeScope)

        return Result(true)
    }

    fun sendOtherSpecialNotifyMessage(
        sendAllNotify: Boolean,
        request: SendNotifyMessageTemplateRequest,
        templateId: String,
        notifyTypeScope: String
    ) {
    }

    override fun completeNotifyMessageByTemplate(request: SendNotifyMessageTemplateRequest): Result<Boolean> {
        // TODO("core暂无实现,需要支持时添加")
        return Result(true)
    }

    override fun getNotifyMessageByTemplate(request: NotifyMessageContextRequest): Result<NotifyContext?> {
        logger.info(
            "getNotifyMessageByTemplate|templateCode=${request.templateCode}|" +
                "notifyTypeEnum=${request.notifyType.name}|" +
                "titleParams=${request.titleParams}|bodyParams=${request.bodyParams}"
        )
        // 1.查出消息模板
        val commonNotifyMessageTemplateRecord =
            commonNotifyMessageTemplateDao.getCommonNotifyMessageTemplateByCode(dslContext, request.templateCode)
                ?: return MessageCodeUtil.generateResponseDataObject(
                    CommonMessageCode.PARAMETER_IS_INVALID,
                    arrayOf(request.templateCode),
                    null
                )

        val notifyContext = when (request.notifyType.name) {
            NotifyType.EMAIL.name -> {
                val emailTplRecord = notifyMessageTemplateDao.getEmailNotifyMessageTemplate(
                    dslContext,
                    commonNotifyMessageTemplateRecord.id
                )!!
                val title = replaceContentParams(request.titleParams, emailTplRecord.title)
                val body = replaceContentParams(request.bodyParams, emailTplRecord.body)
                NotifyContext(title, body)
            }
            NotifyType.RTX.name -> {
                val rtxTplRecord = notifyMessageTemplateDao.getRtxNotifyMessageTemplate(
                    dslContext = dslContext,
                    commonTemplateId = commonNotifyMessageTemplateRecord.id
                )!!
                val title = replaceContentParams(request.titleParams, rtxTplRecord.title)
                val body = replaceContentParams(request.bodyParams, rtxTplRecord.body)
                NotifyContext(title, body)
            }
            NotifyType.WECHAT.name -> {
                val wechatTplRecord = notifyMessageTemplateDao.getWechatNotifyMessageTemplate(
                    dslContext = dslContext,
                    commonTemplateId = commonNotifyMessageTemplateRecord.id
                )!!
                val title = replaceContentParams(request.titleParams, wechatTplRecord.title)
                val body = replaceContentParams(request.bodyParams, wechatTplRecord.body)
                NotifyContext(title, body)
            }
            else -> null
        }
        return Result(notifyContext)
    }

    private fun sendRtxNotifyMessage(
        commonNotifyMessageTemplate: TCommonNotifyMessageTemplateRecord,
        sendNotifyMessageTemplateRequest: SendNotifyMessageTemplateRequest,
        title: String,
        body: String,
        sender: String
    ) {
        logger.info("sendRtxNotifyMessage:\ntitle:$title,\nbody:$body")
        val rtxNotifyMessage = RtxNotifyMessage()
        rtxNotifyMessage.sender = sender
        rtxNotifyMessage.addAllReceivers(sendNotifyMessageTemplateRequest.receivers)
        // 企业微信通知触发人
        val triggerUserId = sendNotifyMessageTemplateRequest.bodyParams?.get("cc")
        if (null != triggerUserId && "" != triggerUserId &&
            !sendNotifyMessageTemplateRequest.receivers.contains(triggerUserId)
        ) {
            rtxNotifyMessage.addReceiver(triggerUserId)
        }
        rtxNotifyMessage.title = title
        rtxNotifyMessage.body = body
        rtxNotifyMessage.priority = EnumNotifyPriority.parse(commonNotifyMessageTemplate.priority.toString())
        rtxNotifyMessage.source = EnumNotifySource.parse(commonNotifyMessageTemplate.source.toInt())
            ?: EnumNotifySource.BUSINESS_LOGIC
        rtxService.sendMqMsg(rtxNotifyMessage)
    }

    private fun sendWechatNotifyMessage(
        commonNotifyMessageTemplate: TCommonNotifyMessageTemplateRecord,
        sendNotifyMessageTemplateRequest: SendNotifyMessageTemplateRequest,
        body: String,
        sender: String
    ) {
        logger.info("sendWechatNotifyMessage:\nbody:$body")
        val wechatNotifyMessage = WechatNotifyMessage()
        wechatNotifyMessage.sender = sender
        wechatNotifyMessage.addAllReceivers(sendNotifyMessageTemplateRequest.receivers)
        wechatNotifyMessage.body = body
        wechatNotifyMessage.priority = EnumNotifyPriority.parse(commonNotifyMessageTemplate.priority.toString())
        wechatNotifyMessage.source = EnumNotifySource.parse(commonNotifyMessageTemplate.source.toInt())
            ?: EnumNotifySource.BUSINESS_LOGIC
        wechatService.sendMqMsg(wechatNotifyMessage)
    }

    private fun sendEmailNotifyMessage(
        commonNotifyMessageTemplate: TCommonNotifyMessageTemplateRecord,
        sendNotifyMessageTemplateRequest: SendNotifyMessageTemplateRequest,
        title: String,
        body: String,
        sender: String
    ) {
        logger.info("sendEmailNotifyMessage:\ntitle:$title,\nbody:$body")
        val commonTemplateId = commonNotifyMessageTemplate.id
        val emailNotifyMessageTemplate =
            notifyMessageTemplateDao.getEmailNotifyMessageTemplate(dslContext, commonTemplateId)
        val emailNotifyMessage = EmailNotifyMessage()
        emailNotifyMessage.sender = sender
        emailNotifyMessage.addAllReceivers(sendNotifyMessageTemplateRequest.receivers)
        val cc = sendNotifyMessageTemplateRequest.cc
        if (null != cc) {
            emailNotifyMessage.addAllCcs(cc)
        }
        val bcc = sendNotifyMessageTemplateRequest.bcc
        if (null != bcc) {
            emailNotifyMessage.addAllBccs(bcc)
        }
        emailNotifyMessage.title = title
        emailNotifyMessage.body = body
        emailNotifyMessage.priority = EnumNotifyPriority.parse(commonNotifyMessageTemplate.priority.toString())
        emailNotifyMessage.source = EnumNotifySource.parse(commonNotifyMessageTemplate.source.toInt())
            ?: EnumNotifySource.BUSINESS_LOGIC
        emailNotifyMessage.format = EnumEmailFormat.parse(emailNotifyMessageTemplate!!.bodyFormat.toInt())
        emailNotifyMessage.type = EnumEmailType.parse(emailNotifyMessageTemplate.emailType.toInt())
        emailService.sendMqMsg(emailNotifyMessage)
    }

    private fun sendWeworkNotifyMessage(
        commonNotifyMessageTemplate: TCommonNotifyMessageTemplateRecord,
        sendNotifyMessageTemplateRequest: SendNotifyMessageTemplateRequest,
        body: String,
        sender: String
    ) {
        val wechatNotifyMessage = WeworkNotifyMessageWithOperation()
        wechatNotifyMessage.sender = sender
        wechatNotifyMessage.addAllReceivers(findWeworkUser(sendNotifyMessageTemplateRequest.receivers))
        wechatNotifyMessage.body = body
        wechatNotifyMessage.priority = EnumNotifyPriority.parse(commonNotifyMessageTemplate.priority.toString())
        wechatNotifyMessage.source = EnumNotifySource.parse(commonNotifyMessageTemplate.source.toInt())
            ?: EnumNotifySource.BUSINESS_LOGIC
        weworkService.sendMqMsg(wechatNotifyMessage)
    }

    protected fun replaceContentParams(params: Map<String, String>?, content: String): String {
        var content1 = content
        params?.forEach { paramName, paramValue ->
            content1 = content1.replace("\${$paramName}", paramValue).replace("#{$paramName}", paramValue)
                .replace("{{$paramName}}", paramValue)
        }
        return content1
    }

    // #5318 为解决使用蓝鲸用户中心生成了带域名的用户名无法与企业微信账号对齐问题
    private fun findWeworkUser(userSet: Set<String>): Set<String> {
        if (userUseDomain!!) {
            val weworkUserSet = mutableSetOf<String>()
            userSet.forEach {
                // 若用户名包含域,取域前的用户名.
                if (it.contains("@")) {
                    weworkUserSet.add(it.substringBefore("@"))
                } else {
                    weworkUserSet.add(it)
                }
            }
            return weworkUserSet
        }
        return userSet
    }
}
