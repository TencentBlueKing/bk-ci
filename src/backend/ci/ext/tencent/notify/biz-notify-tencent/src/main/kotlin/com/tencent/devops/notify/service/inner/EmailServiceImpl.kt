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
package com.tencent.devops.notify.service.inner

import com.google.common.collect.Lists
import com.google.common.collect.Sets
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.notify.enums.EnumEmailFormat
import com.tencent.devops.common.notify.enums.EnumEmailType
import com.tencent.devops.common.notify.enums.EnumNotifyPriority
import com.tencent.devops.common.notify.enums.EnumNotifySource
import com.tencent.devops.common.notify.pojo.EmailNotifyPost
import com.tencent.devops.common.notify.utils.NotifyDigestUtils
import com.tencent.devops.common.notify.utils.TOF4Service
import com.tencent.devops.common.notify.utils.TOF4Service.Companion.TOF4_EMAIL_URL
import com.tencent.devops.common.notify.utils.TOF4Service.Companion.TOF4_EMAIL_URL_WITH_ATTACH
import com.tencent.devops.common.notify.utils.TOFConfiguration
import com.tencent.devops.common.notify.utils.TOFService
import com.tencent.devops.common.notify.utils.TOFService.Companion.EMAIL_URL
import com.tencent.devops.model.notify.tables.records.TNotifyEmailRecord
import com.tencent.devops.notify.EXCHANGE_NOTIFY
import com.tencent.devops.notify.ROUTE_EMAIL
import com.tencent.devops.notify.dao.EmailNotifyDao
import com.tencent.devops.notify.model.EmailNotifyMessageWithOperation
import com.tencent.devops.notify.pojo.EmailNotifyMessage
import com.tencent.devops.notify.pojo.NotificationResponse
import com.tencent.devops.notify.pojo.NotificationResponseWithPage
import com.tencent.devops.notify.service.EmailService
import com.tencent.devops.notify.utils.TofUtil
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.stream.Collectors

@Service
class EmailServiceImpl @Autowired constructor(
    private val tofService: TOFService,
    private val emailNotifyDao: EmailNotifyDao,
    private val rabbitTemplate: RabbitTemplate,
    private val configuration: TOFConfiguration,
    private val tof4Service: TOF4Service
) : EmailService {

    private val logger = LoggerFactory.getLogger(EmailServiceImpl::class.java)
    private var tof4Host: String? = configuration.getDefaultSystem()?.get("host-tof4")
    private var tof4EncryptKey: String? = configuration.getDefaultSystem()?.get("encrypt-key-tof4")

    override fun sendMqMsg(message: EmailNotifyMessage) {
        rabbitTemplate.convertAndSend(EXCHANGE_NOTIFY, ROUTE_EMAIL, message)
    }

    @SuppressWarnings("ComplexMethod")
    override fun sendMessage(emailNotifyMessageWithOperation: EmailNotifyMessageWithOperation) {
        val emailNotifyPost = generateEmailNotifyPost(emailNotifyMessageWithOperation)
        if (emailNotifyPost == null) {
            logger.warn("EmailNotifyPost is empty after being processed, $emailNotifyMessageWithOperation")
            return
        }

        if (emailNotifyMessageWithOperation.getReceivers().isEmpty()) {
            logger.warn("email receivers is empty")
            return
        }

        val tofConfig = TofUtil.getTofConfig(emailNotifyMessageWithOperation, configuration)
        if (tofConfig == null) {
            logger.info("null tofConfig , $emailNotifyMessageWithOperation")
            return
        }
        val id = emailNotifyMessageWithOperation.id ?: UUIDUtil.generate()
        val retryCount = emailNotifyMessageWithOperation.retryCount
        val result = if (tofConfig["tof4Enabled"] == "true") {
            if (emailNotifyPost.codeccAttachFileContent != null) {
                tof4Service.postCodeccEmailFormData(TOF4_EMAIL_URL_WITH_ATTACH, emailNotifyPost, tofConfig)
            } else {
                tof4Service.post(TOF4_EMAIL_URL, emailNotifyPost, tofConfig)
            }
        } else {
            if (emailNotifyPost.codeccAttachFileContent != null) {
                tofService.postCodeccEmailFormData(EMAIL_URL, emailNotifyPost, tofConfig)
            } else {
                tofService.post(EMAIL_URL, emailNotifyPost, tofConfig)
            }
        }

        emailNotifyDao.insertOrUpdateEmailNotifyRecord(
            success = result.Ret == 0,
            source = emailNotifyMessageWithOperation.source,
            id = id,
            retryCount = retryCount,
            lastErrorMessage = if (result.Ret == 0) null else result.ErrMsg,
            to = emailNotifyPost.to,
            cc = emailNotifyPost.cc,
            bcc = emailNotifyPost.bcc,
            sender = emailNotifyPost.from,
            title = emailNotifyPost.title,
            body = emailNotifyPost.content,
            type = emailNotifyPost.emailType,
            format = emailNotifyPost.bodyFormat,
            priority = emailNotifyPost.priority.toInt(),
            contentMd5 = emailNotifyPost.contentMd5,
            frequencyLimit = emailNotifyPost.frequencyLimit,
            tofSysId = tofConfig["sys-id"],
            fromSysId = emailNotifyPost.fromSysId
        )

        if (result.Ret != 0 && retryCount < 3) {
            // 开始重试
            reSendMessage(
                post = emailNotifyPost,
                source = emailNotifyMessageWithOperation.source,
                retryCount = retryCount + 1,
                id = id,
                v2ExtInfo = emailNotifyMessageWithOperation.v2ExtInfo
            )
        }
    }

    private fun reSendMessage(
        post: EmailNotifyPost,
        source: EnumNotifySource,
        retryCount: Int,
        id: String,
        v2ExtInfo: String
    ) {
        val emailNotifyMessageWithOperation = EmailNotifyMessageWithOperation()
        emailNotifyMessageWithOperation.apply {
            this.id = id
            this.retryCount = retryCount
            this.source = source
            title = post.title
            body = post.content
            sender = post.from
            addAllReceivers(Sets.newHashSet(post.to.split(",")))
            addAllCcs(Sets.newHashSet(post.cc.split(",")))
            addAllBccs(Sets.newHashSet(post.bcc.split(",")))
            sender = post.from
            type = EnumEmailType.parse(post.emailType)
            format = EnumEmailFormat.parse(post.bodyFormat)
            priority = EnumNotifyPriority.parse(post.priority)
            frequencyLimit = post.frequencyLimit
            tofSysId = post.tofSysId
            fromSysId = post.fromSysId
            this.v2ExtInfo = v2ExtInfo
        }
        rabbitTemplate.convertAndSend(
            EXCHANGE_NOTIFY,
            ROUTE_EMAIL,
            emailNotifyMessageWithOperation
        ) { message ->
            var delayTime = 0
            when (retryCount) {
                1 -> delayTime = 30000
                2 -> delayTime = 120000
                3 -> delayTime = 300000
            }
            if (delayTime > 0) {
                message.messageProperties.setHeader("x-delay", delayTime)
            }
            message
        }
    }

    private fun generateEmailNotifyPost(emailNotifyMessage: EmailNotifyMessage): EmailNotifyPost? {
        // 由于 soda 中 cc 与 bcc 基本没人使用，暂且不对 cc 和 bcc 作频率限制
        val contentMd5 = NotifyDigestUtils.getMessageContentMD5("", emailNotifyMessage.body)
        val tos = Lists.newArrayList(
            filterReceivers(
                receivers = emailNotifyMessage.getReceivers(),
                contentMd5 = contentMd5,
                frequencyLimit = emailNotifyMessage.frequencyLimit
            )
        )
        val ccs = emailNotifyMessage.getCc()
        val bccs = emailNotifyMessage.getBcc()
        if (tos.isEmpty() && ccs.isEmpty() && bccs.isEmpty()) {
            return null
        }

        val post = EmailNotifyPost()
        post.apply {
            title = emailNotifyMessage.title
            from = emailNotifyMessage.sender
            if (tos.isNotEmpty()) {
                to = tos.joinToString(",")
            }
            if (ccs.isNotEmpty()) {
                cc = ccs.joinToString(",")
            }
            if (bccs.isNotEmpty()) {
                bcc = bccs.joinToString(",")
            }
            priority = emailNotifyMessage.priority.getValue()
            bodyFormat = emailNotifyMessage.format.getValue()
            emailType = emailNotifyMessage.type.getValue()
            content = emailNotifyMessage.body
            this.contentMd5 = contentMd5
            frequencyLimit = emailNotifyMessage.frequencyLimit
            tofSysId = emailNotifyMessage.tofSysId
            fromSysId = emailNotifyMessage.fromSysId
            codeccAttachFileContent = emailNotifyMessage.codeccAttachFileContent
        }

        return post
    }

    @SuppressWarnings("NestedBlockDepth")
    private fun filterReceivers(
        receivers: Set<String>,
        contentMd5: String,
        frequencyLimit: Int
    ): Set<String> {
        val filteredReceivers = HashSet(receivers)
        val filteredOutReceivers = HashSet<String>()
        if (frequencyLimit > 0) {
            val recordedReceivers = emailNotifyDao.getTosByContentMd5AndTime(
                contentMd5, (frequencyLimit * 60).toLong()
            )
            receivers.forEach { rec ->
                for (recordedRec in recordedReceivers) {
                    if (",$recordedRec,".contains(rec)) {
                        filteredReceivers.remove(rec)
                        filteredOutReceivers.add(rec)
                        break
                    }
                }
            }
            logger.warn("Filtered out receivers:$filteredOutReceivers")
        }
        return filteredReceivers
    }

    override fun listByCreatedTime(
        page: Int,
        pageSize: Int,
        success: Boolean?,
        fromSysId: String?,
        createdTimeSortOrder: String?
    ): NotificationResponseWithPage<EmailNotifyMessageWithOperation> {
        val count = emailNotifyDao.count(success, fromSysId)
        val result: List<NotificationResponse<EmailNotifyMessageWithOperation>> = if (count == 0) {
            listOf()
        } else {
            val emailRecords = emailNotifyDao.list(
                page = page,
                pageSize = pageSize,
                success = success,
                fromSysId = fromSysId,
                createdTimeSortOrder = createdTimeSortOrder
            )
            emailRecords.stream().map(this::parseFromTNotifyEmailToResponse)?.collect(Collectors.toList())
                ?: listOf()
        }
        return NotificationResponseWithPage(
            count = count,
            page = page,
            pageSize = pageSize,
            data = result
        )
    }

    private fun parseFromTNotifyEmailToResponse(
        record: TNotifyEmailRecord
    ): NotificationResponse<EmailNotifyMessageWithOperation> {
        val receivers: MutableSet<String> = mutableSetOf()
        if (!record.to.isNullOrEmpty()) {
            receivers.addAll(record.to.split(";"))
        }
        val cc: MutableSet<String> = mutableSetOf()
        if (!record.cc.isNullOrEmpty()) {
            cc.addAll(record.cc.split(";"))
        }
        val bcc: MutableSet<String> = mutableSetOf()
        if (!record.bcc.isNullOrEmpty()) {
            bcc.addAll(record.bcc.split(";"))
        }

        val message = EmailNotifyMessageWithOperation()
        message.apply {
            frequencyLimit = record.frequencyLimit
            fromSysId = record.fromSysId
            tofSysId = record.tofSysId
            format = EnumEmailFormat.parse(record.format)
            type = EnumEmailType.parse(record.type)
            body = record.body
            sender = record.sender
            title = record.title
            priority = EnumNotifyPriority.parse(record.priority.toString())
            source = EnumNotifySource.parseName(record.source)
            retryCount = record.retryCount
            lastError = record.lastError
            addAllReceivers(receivers)
            addAllCcs(cc)
            addAllBccs(bcc)
        }

        return NotificationResponse(
            id = record.id,
            success = record.success,
            createdTime = if (record.createdTime == null) {
                null
            } else {
                DateTimeUtil.convertLocalDateTimeToTimestamp(record.createdTime)
            },
            updatedTime = if (record.updatedTime == null) {
                null
            } else {
                DateTimeUtil.convertLocalDateTimeToTimestamp(record.updatedTime)
            },
            contentMD5 = record.contentMd5,
            notificationMessage = message
        )
    }
}
