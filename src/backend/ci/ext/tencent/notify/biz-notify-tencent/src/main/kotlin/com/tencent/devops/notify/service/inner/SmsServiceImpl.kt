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
import com.tencent.devops.common.notify.enums.EnumNotifyPriority
import com.tencent.devops.common.notify.enums.EnumNotifySource
import com.tencent.devops.common.notify.pojo.SmsNotifyPost
import com.tencent.devops.common.notify.utils.ChineseStringUtil
import com.tencent.devops.common.notify.utils.NotifyDigestUtils
import com.tencent.devops.common.notify.utils.TOFConfiguration
import com.tencent.devops.common.notify.utils.TOFService
import com.tencent.devops.common.notify.utils.TOFService.Companion.SMS_URL
import com.tencent.devops.model.notify.tables.records.TNotifySmsRecord
import com.tencent.devops.notify.EXCHANGE_NOTIFY
import com.tencent.devops.notify.ROUTE_SMS
import com.tencent.devops.notify.dao.SmsNotifyDao
import com.tencent.devops.notify.model.SmsNotifyMessageWithOperation
import com.tencent.devops.notify.pojo.NotificationResponse
import com.tencent.devops.notify.pojo.NotificationResponseWithPage
import com.tencent.devops.notify.pojo.SmsNotifyMessage
import com.tencent.devops.notify.service.SmsService
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.LinkedList
import java.util.stream.Collectors

@Service
class SmsServiceImpl @Autowired constructor(
    private val tofService: TOFService,
    private val smsNotifyDao: SmsNotifyDao,
    private val rabbitTemplate: RabbitTemplate,
    private val tofConfiguration: TOFConfiguration
) : SmsService {

    private val logger = LoggerFactory.getLogger(SmsServiceImpl::class.java)

    @Value("\${tof.defaultSystem.default-sms-sender}")
    private lateinit var defaultSmsSender: String

    override fun sendMqMsg(message: SmsNotifyMessage) {
        logger.info("sms channel has been closed , message is $message")
        return
        // 短信渠道下架
        // 后续容器化之后 , 不会再有这份代码
        // rabbitTemplate.convertAndSend(EXCHANGE_NOTIFY, ROUTE_SMS, message)
    }

    override fun sendMessage(smsNotifyMessageWithOperation: SmsNotifyMessageWithOperation) {
        val smsNotifyPosts = generateSmsNotifyPost(smsNotifyMessageWithOperation)
        if (smsNotifyPosts.isEmpty()) {
            logger.warn("List<SmsNotifyPost> is empty after being processed, SmsNotifyMessageWithOperation: $smsNotifyMessageWithOperation")
            return
        }

        val retryCount = smsNotifyMessageWithOperation.retryCount
        val batchId = smsNotifyMessageWithOperation.batchId ?: UUIDUtil.generate()
        val tofConfs = tofConfiguration.getConfigurations(smsNotifyMessageWithOperation.tofSysId)
        for (notifyPost in smsNotifyPosts) {
            val id = smsNotifyMessageWithOperation.id ?: UUIDUtil.generate()
            val result = tofService.post(
                SMS_URL, notifyPost, tofConfs!!
            )
            if (result.Ret == 0) {
                // 成功
                smsNotifyDao.insertOrUpdateSmsNotifyRecord(
                    success = true,
                    source = smsNotifyMessageWithOperation.source,
                    batchId = batchId,
                    id = id,
                    retryCount = retryCount,
                    lastErrorMessage = null,
                    receivers = notifyPost.receiver,
                    sender = notifyPost.sender,
                    body = notifyPost.msgInfo,
                    priority = notifyPost.priority.toInt(),
                    contentMd5 = notifyPost.contentMd5,
                    frequencyLimit = notifyPost.frequencyLimit,
                    tofSysId = tofConfs["sys-id"],
                    fromSysId = notifyPost.fromSysId
                )
            } else {
                // 写入失败记录
                smsNotifyDao.insertOrUpdateSmsNotifyRecord(
                    success = false,
                    source = smsNotifyMessageWithOperation.source,
                    batchId = batchId,
                    id = id,
                    retryCount = retryCount,
                    lastErrorMessage = result.ErrMsg,
                    receivers = notifyPost.receiver,
                    sender = notifyPost.sender,
                    body = notifyPost.msgInfo,
                    priority = notifyPost.priority.toInt(),
                    contentMd5 = notifyPost.contentMd5,
                    frequencyLimit = notifyPost.frequencyLimit,
                    tofSysId = tofConfs["sys-id"],
                    fromSysId = notifyPost.fromSysId
                )
                if (retryCount < 3) {
                    // 开始重试
                    reSendMessage(
                        post = notifyPost,
                        source = smsNotifyMessageWithOperation.source,
                        retryCount = retryCount + 1,
                        id = id,
                        batchId = batchId
                    )
                }
            }
        }
    }

    private fun reSendMessage(
        post: SmsNotifyPost,
        source: EnumNotifySource,
        retryCount: Int,
        id: String?,
        batchId: String?
    ) {
        val smsNotifyMessageWithOperation = SmsNotifyMessageWithOperation()
        smsNotifyMessageWithOperation.apply {
            this.batchId = batchId
            this.id = id
            this.retryCount = retryCount
            this.source = source
            sender = post.sender
            addAllReceivers(Sets.newHashSet(post.receiver.split(",")))
            priority = EnumNotifyPriority.parse(post.priority)
            body = post.msgInfo
            frequencyLimit = post.frequencyLimit
            tofSysId = post.tofSysId
            fromSysId = post.fromSysId
        }
        rabbitTemplate.convertAndSend(EXCHANGE_NOTIFY, ROUTE_SMS, smsNotifyMessageWithOperation) { message ->
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

    private fun generateSmsNotifyPost(smsNotifyMessage: SmsNotifyMessage): List<SmsNotifyPost> {
        val list = LinkedList<SmsNotifyPost>()
        val bodyList = ChineseStringUtil.split(smsNotifyMessage.body, 220) ?: return list
        for (i in bodyList.indices) {
            val body = if (bodyList.size > 1) {
                String.format(
                    "%s(%d/%d)",
                    bodyList[i],
                    i + 1,
                    bodyList.size
                )
            } else {
                bodyList[i]
            }

            val contentMd5 = NotifyDigestUtils.getMessageContentMD5("", body)
            val receivers = Lists.newArrayList(
                filterReceivers(
                    receivers = smsNotifyMessage.getReceivers(),
                    contentMd5 = contentMd5,
                    frequencyLimit = smsNotifyMessage.frequencyLimit
                )
            )
            if (receivers == null || receivers.isEmpty()) {
                continue
            }

            for (j in 0 until receivers.size / 100 + 1) {
                val startIndex = j * 100
                var toIndex = j * 100 + 99
                if (toIndex > receivers.size - 1) {
                    toIndex = receivers.size
                }
                val subReceivers = receivers.subList(startIndex, toIndex)
                val post = SmsNotifyPost()
                post.apply {
                    receiver = subReceivers.joinToString(",")
                    msgInfo = body
                    priority = smsNotifyMessage.priority.getValue()
                    sender = if (smsNotifyMessage.sender.isEmpty()) {
                        defaultSmsSender
                    } else {
                        smsNotifyMessage.sender
                    }
                    this.contentMd5 = contentMd5
                    frequencyLimit = smsNotifyMessage.frequencyLimit
                    tofSysId = smsNotifyMessage.tofSysId
                    fromSysId = smsNotifyMessage.fromSysId
                }
                list.add(post)
            }
        }
        return list
    }

    private fun filterReceivers(receivers: Set<String>, contentMd5: String, frequencyLimit: Int): Set<String> {
        val filteredReceivers = HashSet(receivers)
        val filteredOutReceivers = HashSet<String>()
        if (frequencyLimit > 0) {
            val recordedReceivers = smsNotifyDao.getReceiversByContentMd5AndTime(
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
    ): NotificationResponseWithPage<SmsNotifyMessageWithOperation> {
        val count = smsNotifyDao.count(success, fromSysId)
        val result: List<NotificationResponse<SmsNotifyMessageWithOperation>> = if (count == 0) {
            listOf()
        } else {
            val emailRecords = smsNotifyDao.list(
                page = page,
                pageSize = pageSize,
                success = success,
                fromSysId = fromSysId,
                createdTimeSortOrder = createdTimeSortOrder
            )
            emailRecords.stream().map(this::parseFromTNotifySmsToResponse)?.collect(Collectors.toList()) ?: listOf()
        }
        return NotificationResponseWithPage(
            count = count,
            page = page,
            pageSize = pageSize,
            data = result
        )
    }

    private fun parseFromTNotifySmsToResponse(record: TNotifySmsRecord): NotificationResponse<SmsNotifyMessageWithOperation> {
        val receivers: MutableSet<String> = mutableSetOf()
        if (!record.receivers.isNullOrEmpty())
            receivers.addAll(record.receivers.split(";"))

        val message = SmsNotifyMessageWithOperation()
        message.apply {
            frequencyLimit = record.frequencyLimit
            fromSysId = record.fromSysId
            tofSysId = record.tofSysId
            body = record.body
            sender = record.sender
            priority = EnumNotifyPriority.parse(record.priority.toString())
            source = EnumNotifySource.parseName(record.source)
            retryCount = record.retryCount
            lastError = record.lastError
            addAllReceivers(receivers)
        }

        return NotificationResponse(
            record.id, record.success,
            if (record.createdTime == null) null
            else
                DateTimeUtil.convertLocalDateTimeToTimestamp(record.createdTime),
            if (record.updatedTime == null) null
            else
                DateTimeUtil.convertLocalDateTimeToTimestamp(record.updatedTime),
            record.contentMd5, message
        )
    }
}
