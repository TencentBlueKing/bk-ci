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
package com.tencent.devops.notify.blueking.service.inner

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
import com.tencent.devops.common.notify.utils.Configuration
import com.tencent.devops.model.notify.tables.records.TNotifyEmailRecord
import com.tencent.devops.notify.EXCHANGE_NOTIFY
import com.tencent.devops.notify.ROUTE_EMAIL
import com.tencent.devops.notify.blueking.utils.NotifyService
import com.tencent.devops.notify.blueking.utils.NotifyService.Companion.EMAIL_URL
import com.tencent.devops.notify.dao.EmailNotifyDao
import com.tencent.devops.notify.model.EmailNotifyMessageWithOperation
import com.tencent.devops.notify.pojo.EmailNotifyMessage
import com.tencent.devops.notify.pojo.NotificationResponse
import com.tencent.devops.notify.pojo.NotificationResponseWithPage
import com.tencent.devops.notify.service.EmailService
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import java.util.stream.Collectors

@Suppress("ALL")
class EmailServiceImpl @Autowired constructor(
    private val notifyService: NotifyService,
    private val emailNotifyDao: EmailNotifyDao,
    private val rabbitTemplate: RabbitTemplate,
    private val configuration: Configuration
) : EmailService {

    private val logger = LoggerFactory.getLogger(EmailServiceImpl::class.java)

    override fun sendMqMsg(message: EmailNotifyMessage) {
        rabbitTemplate.convertAndSend(EXCHANGE_NOTIFY, ROUTE_EMAIL, message)
    }

    override fun sendMessage(emailNotifyMessageWithOperation: EmailNotifyMessageWithOperation) {
        val emailNotifyPost = generateEmailNotifyPost(emailNotifyMessageWithOperation)
        if (emailNotifyPost == null) {
            logger.warn("EmailNotifyPost is empty after being processed: $emailNotifyMessageWithOperation")
            return
        }

        val retryCount = emailNotifyMessageWithOperation.retryCount
        val id = emailNotifyMessageWithOperation.id ?: UUIDUtil.generate()
        val tofConfs = configuration.getConfigurations(emailNotifyMessageWithOperation.tofSysId)
        val result = notifyService.post(
            EMAIL_URL, emailNotifyPost, tofConfs!!)
        if (result.Ret == 0) {
            // 成功
            emailNotifyDao.insertOrUpdateEmailNotifyRecord(true, emailNotifyMessageWithOperation.source, id,
                retryCount, null, emailNotifyPost.to, emailNotifyPost.cc, emailNotifyPost.bcc, emailNotifyPost.from,
                emailNotifyPost.title, emailNotifyPost.content, emailNotifyPost.emailType, emailNotifyPost.bodyFormat,
                emailNotifyPost.priority.toInt(), emailNotifyPost.contentMd5, emailNotifyPost.frequencyLimit,
                tofConfs["sys-id"], emailNotifyPost.fromSysId)
        } else {
            // 写入失败记录
            emailNotifyDao.insertOrUpdateEmailNotifyRecord(
                success = false,
                source = emailNotifyMessageWithOperation.source,
                id = id,
                retryCount = retryCount,
                lastErrorMessage = result.ErrMsg,
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
                tofSysId = tofConfs["sys-id"],
                fromSysId = emailNotifyPost.fromSysId
            )
            if (retryCount < 3) {
                // 开始重试
                reSendMessage(
                    post = emailNotifyPost,
                    source = emailNotifyMessageWithOperation.source,
                    retryCount = retryCount + 1,
                    id = id
                )
            }
        }
    }

    private fun reSendMessage(post: EmailNotifyPost, source: EnumNotifySource, retryCount: Int, id: String) {
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
        }
        rabbitTemplate.convertAndSend(EXCHANGE_NOTIFY, ROUTE_EMAIL, emailNotifyMessageWithOperation) { message ->
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

        val contentMd5 = NotifyDigestUtils.getMessageContentMD5("", emailNotifyMessage.body)
        val tos = Lists.newArrayList(filterReceivers(
            emailNotifyMessage.getReceivers(), contentMd5, emailNotifyMessage.frequencyLimit)
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
        }

        return post
    }

    private fun filterReceivers(receivers: Set<String>, contentMd5: String, frequencyLimit: Int): Set<String> {
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
            val emailRecords = emailNotifyDao.list(page, pageSize, success, fromSysId, createdTimeSortOrder)
            emailRecords.stream().map(this::parseFromTNotifyEmailToResponse)?.collect(Collectors.toList()) ?: listOf()
        }
        return NotificationResponseWithPage(count, page, pageSize, result)
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

        return NotificationResponse(id = record.id, success = record.success,
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
            contentMD5 = record.contentMd5, notificationMessage = message)
    }
}
