/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.notify.tables.records.TCommonNotifyMessageTemplateRecord
import com.tencent.devops.model.notify.tables.records.TEmailsNotifyMessageTemplateRecord
import com.tencent.devops.model.notify.tables.records.TVoiceNotifyMessageTemplateRecord
import com.tencent.devops.model.notify.tables.records.TWechatNotifyMessageTemplateRecord
import com.tencent.devops.model.notify.tables.records.TWeworkGroupNotifyMessageTemplateRecord
import com.tencent.devops.model.notify.tables.records.TWeworkNotifyMessageTemplateRecord
import com.tencent.devops.notify.dao.CommonNotifyMessageTemplateDao
import com.tencent.devops.notify.dao.MessageTemplateDao
import com.tencent.devops.notify.dao.NotifyMessageTemplateDao
import com.tencent.devops.notify.pojo.NotifyContext
import com.tencent.devops.notify.pojo.NotifyMessageCommonTemplate
import com.tencent.devops.notify.pojo.NotifyMessageContextRequest
import com.tencent.devops.notify.pojo.NotifyTemplateMessage
import com.tencent.devops.notify.pojo.NotifyTemplateMessageRequest
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.notify.pojo.SubNotifyMessageTemplate
import com.tencent.devops.notify.pojo.messageTemplate.MessageTemplate
import com.tencent.devops.notify.service.notifier.INotifier
import com.tencent.devops.notify.service.notifier.NotifierUtils
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.io.File
import java.time.LocalDateTime
import java.util.concurrent.Executors
import jakarta.annotation.PostConstruct

@Service
@Suppress("ALL")
class NotifyMessageTemplateServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val notifyMessageTemplateDao: NotifyMessageTemplateDao,
    private val commonNotifyMessageTemplateDao: CommonNotifyMessageTemplateDao,
    private val redisOperation: RedisOperation,
    private val messageTemplateDao: MessageTemplateDao,
    private val commonConfig: CommonConfig
) : NotifyMessageTemplateService {

    companion object {
        private val logger = LoggerFactory.getLogger(NotifyMessageTemplateServiceImpl::class.java)
    }

    @PostConstruct
    fun init() {
        val redisLock = RedisLock(
            redisOperation = redisOperation,
            lockKey = "NOTIFY_MESSAGE_TEMPLATE_INIT_LOCK",
            expiredTimeInSeconds = 60

        )
        val traceId = MDC.get(TraceTag.BIZID)
        Executors.newFixedThreadPool(1).submit {
            MDC.put(TraceTag.BIZID, traceId)
            if (redisLock.tryLock()) {
                try {
                    logger.info("start init MessageTemplate")
                    updateMessageTemplate()
                    logger.info("start init MessageTemplate succeed")
                } catch (ignored: Throwable) {
                    logger.warn("start init MessageTemplate fail! error:${ignored.message}")
                } finally {
                    redisLock.unlock()
                }
            }
        }
    }

    fun updateMessageTemplate() {
        val classPathResource = ClassPathResource(
            "i18n${File.separator}template_${commonConfig.devopsDefaultLocaleLanguage}.yaml"
        )
        val inputStream = classPathResource.inputStream
        val yamlStr = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val templates = YamlUtil.to(yamlStr, object : TypeReference<List<MessageTemplate>>() {})
        templates.forEach { template ->
            logger.info("update message template:$template")
            val tCommonNotifyMessageTemplateRecord = TCommonNotifyMessageTemplateRecord()
            tCommonNotifyMessageTemplateRecord.id = template.id
            tCommonNotifyMessageTemplateRecord.templateCode = template.templateCode
            tCommonNotifyMessageTemplateRecord.templateName = template.templateName
            tCommonNotifyMessageTemplateRecord.notifyTypeScope = JsonUtil.toJson(template.notifyTypeScope)
            tCommonNotifyMessageTemplateRecord.priority = template.priority.ordinal.toByte()
            tCommonNotifyMessageTemplateRecord.source = template.source.getValue().toByte()
            messageTemplateDao.createCommonNotifyMessageTemplate(
                dslContext,
                tCommonNotifyMessageTemplateRecord
            )
            val tWechatNotifyMessageTemplateRecord = template.wechatTemplate?.let {
                val wechatTemplate = template.wechatTemplate!!
                TWechatNotifyMessageTemplateRecord().apply {
                    this.id = wechatTemplate.id
                    this.commonTemplateId = template.id
                    this.body = wechatTemplate.body
                    this.title = wechatTemplate.title
                    this.sender = wechatTemplate.sender
                    this.creator = template.creator
                    this.modifior = template.modifior
                    this.createTime = LocalDateTime.now()
                    this.updateTime = LocalDateTime.now()
                }
            }
            val tWeworkGroupNotifyMessageTemplateRecord = template.weworkGroupTemplate?.let {
                val weworkGroupTemplate = template.weworkGroupTemplate!!
                TWeworkGroupNotifyMessageTemplateRecord().apply {
                    this.id = weworkGroupTemplate.id
                    this.commonTemplateId = template.id
                    this.body = weworkGroupTemplate.body
                    this.title = weworkGroupTemplate.title
                    this.creator = template.creator
                    this.modifior = template.modifior
                    this.createTime = LocalDateTime.now()
                    this.updateTime = LocalDateTime.now()
                }
            }
            val tWeworkNotifyMessageTemplateRecord = template.weworkTemplate?.let {
                val weworkTemplate = template.weworkTemplate!!
                TWeworkNotifyMessageTemplateRecord().apply {
                    this.id = weworkTemplate.id
                    this.commonTemplateId = template.id
                    this.body = weworkTemplate.body
                    this.title = weworkTemplate.title
                    this.sender = weworkTemplate.sender
                    this.creator = template.creator
                    this.modifior = template.modifior
                    this.createTime = LocalDateTime.now()
                    this.updateTime = LocalDateTime.now()
                }
            }
            val tEmailsNotifyMessageTemplateRecord = template.emailTemplate?.let {
                val emailTemplate = template.emailTemplate!!
                TEmailsNotifyMessageTemplateRecord().apply {
                    this.id = emailTemplate.id
                    this.commonTemplateId = template.id
                    this.body = emailTemplate.body
                    this.title = emailTemplate.title
                    this.bodyFormat = emailTemplate.bodyFormat?.getValue()?.toByte()
                    this.emailType = emailTemplate.emailType?.getValue()?.toByte()
                    this.creator = template.creator
                    this.modifior = template.modifior
                    this.createTime = LocalDateTime.now()
                    this.updateTime = LocalDateTime.now()
                }
            }
            val tVoiceNotifyMessageTemplateRecord = template.voiceTemplate?.let {
                val voiceTemplate = template.voiceTemplate!!
                TVoiceNotifyMessageTemplateRecord().apply {
                    this.id = voiceTemplate.id
                    this.commonTemplateId = template.id
                    this.creator = template.creator
                    this.modifior = template.modifior
                    this.taskName = voiceTemplate.taskName
                    this.content = voiceTemplate.content
                    this.createTime = LocalDateTime.now()
                    this.updateTime = LocalDateTime.now()
                }
            }

            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                messageTemplateDao.createCommonNotifyMessageTemplate(
                    transactionContext,
                    tCommonNotifyMessageTemplateRecord
                )
                tWechatNotifyMessageTemplateRecord?.let { record ->
                    messageTemplateDao.createWechatNotifyMessageTemplate(transactionContext, record)
                }
                tWeworkNotifyMessageTemplateRecord?.let { record ->
                    messageTemplateDao.createWeworkNotifyMessageTemplate(transactionContext, record)
                }
                tWeworkGroupNotifyMessageTemplateRecord?.let { record ->
                    messageTemplateDao.createWeworkGroupNotifyMessageTemplate(transactionContext, record)
                }
                tEmailsNotifyMessageTemplateRecord?.let { record ->
                    messageTemplateDao.createEmailsNotifyMessageTemplate(transactionContext, record)
                }
                tVoiceNotifyMessageTemplateRecord?.let { record ->
                    messageTemplateDao.createVoiceNotifyMessageTemplate(transactionContext, record)
                }
            }
        }
    }

    /**
     * 根据查找到的消息通知模板主体信息来获取具体信息
     * @param userId
     * @param templateId 对应模板通知模板的ID，在消息通知模板表中
     */
    override fun getNotifyMessageTemplates(userId: String, templateId: String): Result<Page<SubNotifyMessageTemplate>> {
        val email = notifyMessageTemplateDao.getEmailNotifyMessageTemplate(dslContext, templateId)
        val wechat = notifyMessageTemplateDao.getWechatNotifyMessageTemplate(dslContext, templateId)
        val rtx = notifyMessageTemplateDao.getRtxNotifyMessageTemplate(dslContext, templateId)
        val common = notifyMessageTemplateDao.getCommonNotifyMessageTemplatesNotifyType(
            dslContext = dslContext,
            templateId = templateId
        )
        logger.info("common template notify type is $common")
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
                    notifyTypeScope = mutableListOf(NotifyType.RTX.name),
                    title = rtx.title,
                    body = rtx.body,
                    bodyMD = rtx.bodyMd,
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

        getOtherNotifyMessageTemplate(subTemplateList, templateId)

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

    fun getOtherNotifyMessageTemplate(
        subTemplateList: MutableList<SubNotifyMessageTemplate>,
        templateId: String
    ) {
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
                    return I18nUtil.generateResponseDataObject(
                        messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                        params = arrayOf("${it.bodyFormat} or ${it.emailType}"),
                        data = false,
                        language = I18nUtil.getLanguage(userId)
                    )
                }
                notifyTypeScopeSet.add(NotifyType.EMAIL.name)
            }
        }

        // 添加的信息中的模板类型是非法数据
        if (notifyTypeScopeSet.size == 0) {
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf("notifyType"),
                data = false,
                language = I18nUtil.getLanguage(userId)
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
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf("Code/Name"),
                data = false,
                language = I18nUtil.getLanguage(userId)
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
                if (it.notifyTypeScope.contains(NotifyType.VOICE.name)) {
                    notifyMessageTemplateDao.addVoiceNotifyMessageTemplate(
                        dslContext = context,
                        commonTemplateId = id,
                        id = UUIDUtil.generate(),
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
        val notifyTypeScopeSet = mutableSetOf<String>()
        // 判断提交的数据中是否存在同样类型的
        notifyMessageTemplateRequest.msg.forEach {
            for (notifyType in NotifyType.opEditable()) {
                if (it.notifyTypeScope.contains(notifyType.name)) {
                    if (notifyTypeScopeSet.contains(notifyType.name)) {
                        logger.warn("${notifyType.name} has set.")
                        return I18nUtil.generateResponseDataObject(
                            messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                            params = arrayOf("notifyType"),
                            data = false,
                            language = I18nUtil.getLanguage(userId)
                        )
                    }
                    notifyTypeScopeSet.add(notifyType.name)
                }
            }
        }

        if (!updateOtherNotifyMessageTemplate(notifyMessageTemplateRequest, notifyTypeScopeSet)) {
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf("notifyType"),
                data = false,
                language = I18nUtil.getLanguage(userId)
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
                    upsetWechatTemplate(templateId, userId, it, uid)
                }
                if (it.notifyTypeScope.contains(NotifyType.RTX.name)) {
                    upsetRtxTemplate(templateId, userId, it, uid)
                }
                if (it.notifyTypeScope.contains(NotifyType.EMAIL.name)) {
                    upsetEmailTemplate(templateId, userId, it, uid)
                }
                if (it.notifyTypeScope.contains(NotifyType.VOICE.name)) {
                    upsetVoiceTemplate(templateId, userId, it, uid)
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

    override fun updateTXSESTemplateId(userId: String, templateId: String, sesTemplateId: Int?): Result<Boolean> {
        logger.info("updateTXSESTemplateId|$userId|$templateId|$sesTemplateId")
        return Result(notifyMessageTemplateDao.updateTXSESTemplateId(dslContext, templateId, sesTemplateId))
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
            logger.info("get message type：$record")
            val existsNotifyType =
                JsonUtil.getObjectMapper().readValue(record, List::class.java) as ArrayList<String>
            logger.info(
                "delete the message template subtable information:" +
                        "$notifyType ${NotifyType.EMAIL} ${notifyType == NotifyType.EMAIL.name}"
            )
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

                NotifyType.VOICE.name -> {
                    notifyMessageTemplateDao.deleteVoiceNotifyMessageTemplate(context, templateId)
                }
            }

            if (existsNotifyType.size == 1 && existsNotifyType[0] == notifyType) {
                logger.info("Delete common table info")
                notifyMessageTemplateDao.deleteCommonNotifyMessageTemplate(context, templateId)
                return@transaction
            }
            logger.info("Update common table info")
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
        dslContext.transaction { t ->
            val dsl = DSL.using(t)
            notifyMessageTemplateDao.deleteCommonNotifyMessageTemplate(dsl, templateId)
            notifyMessageTemplateDao.deleteEmailsNotifyMessageTemplate(dsl, templateId)
            notifyMessageTemplateDao.deleteRtxNotifyMessageTemplate(dsl, templateId)
            notifyMessageTemplateDao.deleteWechatNotifyMessageTemplate(dsl, templateId)
            notifyMessageTemplateDao.deleteVoiceNotifyMessageTemplate(dsl, templateId)
        }
        return Result(true)
    }

    override fun sendNotifyMessageByTemplate(request: SendNotifyMessageTemplateRequest): Result<Boolean> {
        val templateCode = request.templateCode
        // 查出消息模板
        val commonNotifyMessageTemplateRecord =
            commonNotifyMessageTemplateDao.getCommonNotifyMessageTemplateByCode(dslContext, templateCode)
                ?: return I18nUtil.generateResponseDataObject(
                    messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                    params = arrayOf(templateCode),
                    data = false
                )

        val sendAllNotify = request.notifyType == null
        val notifyTypeScope = commonNotifyMessageTemplateRecord.notifyTypeScope

        val notifiers = SpringContextUtil.getBeansWithClass(INotifier::class.java)
        for (notifier in notifiers) {
            if (sendAllNotify || request.notifyType?.contains(notifier.type().name) == true) {
                if (!notifyTypeScope.contains(notifier.type().name)) {
                    logger.warn(
                        "COMMON_NOTIFY_MESSAGE_TEMPLATE_NOT_FOUND|If needed, add on the OP" +
                                "|type=${notifier.type()}|template=${request.templateCode}"
                    )
                } else {
                    notifier.send(request, commonNotifyMessageTemplateRecord)
                }
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
                ?: return I18nUtil.generateResponseDataObject(
                    messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                    params = arrayOf(request.templateCode),
                    data = null
                )

        val notifyContext = when (request.notifyType.name) {
            NotifyType.EMAIL.name -> {
                val emailTplRecord = notifyMessageTemplateDao.getEmailNotifyMessageTemplate(
                    dslContext,
                    commonNotifyMessageTemplateRecord.id
                )!!
                val title = NotifierUtils.replaceContentParams(request.titleParams, emailTplRecord.title)
                val body = NotifierUtils.replaceContentParams(request.bodyParams, emailTplRecord.body)
                NotifyContext(title, body)
            }

            NotifyType.RTX.name -> {
                val rtxTplRecord = notifyMessageTemplateDao.getRtxNotifyMessageTemplate(
                    dslContext = dslContext,
                    commonTemplateId = commonNotifyMessageTemplateRecord.id
                )!!
                val title = NotifierUtils.replaceContentParams(request.titleParams, rtxTplRecord.title)
                val body = NotifierUtils.replaceContentParams(request.bodyParams, rtxTplRecord.body)
                NotifyContext(title, body)
            }

            NotifyType.WECHAT.name -> {
                val wechatTplRecord = notifyMessageTemplateDao.getWechatNotifyMessageTemplate(
                    dslContext = dslContext,
                    commonTemplateId = commonNotifyMessageTemplateRecord.id
                )!!
                val title = NotifierUtils.replaceContentParams(request.titleParams, wechatTplRecord.title)
                val body = NotifierUtils.replaceContentParams(request.bodyParams, wechatTplRecord.body)
                NotifyContext(title, body)
            }

            NotifyType.VOICE.name -> {
                val voiceTplRecord = notifyMessageTemplateDao.getVoiceNotifyMessageTemplate(
                    dslContext = dslContext,
                    commonTemplateId = commonNotifyMessageTemplateRecord.id
                )!!
                val title = NotifierUtils.replaceContentParams(request.titleParams, voiceTplRecord.taskName)
                val body = NotifierUtils.replaceContentParams(request.bodyParams, voiceTplRecord.content)
                NotifyContext(title, body)
            }

            else -> null
        }
        return Result(notifyContext)
    }

    private fun upsetEmailTemplate(
        templateId: String,
        userId: String,
        it: NotifyTemplateMessage,
        uid: String
    ) {
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

    private fun upsetVoiceTemplate(
        templateId: String,
        userId: String,
        it: NotifyTemplateMessage,
        uid: String
    ) {
        val num = notifyMessageTemplateDao.countVoiceMessageTemplate(dslContext, templateId)
        if (num > 0) {
            notifyMessageTemplateDao.updateVoiceNotifyMessageTemplate(
                dslContext = dslContext,
                userId = userId,
                templateId = templateId,
                notifyTemplateMessage = it
            )
        } else {
            notifyMessageTemplateDao.addVoiceNotifyMessageTemplate(
                dslContext = dslContext,
                commonTemplateId = templateId,
                id = uid,
                userId = userId,
                notifyTemplateMessage = it
            )
        }
    }

    private fun upsetRtxTemplate(
        templateId: String,
        userId: String,
        it: NotifyTemplateMessage,
        uid: String
    ) {
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

    private fun upsetWechatTemplate(
        templateId: String,
        userId: String,
        it: NotifyTemplateMessage,
        uid: String
    ) {
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
}
