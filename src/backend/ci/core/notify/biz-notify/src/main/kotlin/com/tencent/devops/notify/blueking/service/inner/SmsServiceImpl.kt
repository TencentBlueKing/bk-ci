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
package com.tencent.devops.notify.blueking.service.inner

import com.google.common.collect.Lists
import com.google.common.collect.Sets
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.notify.enums.EnumNotifyPriority
import com.tencent.devops.common.notify.enums.EnumNotifySource
import com.tencent.devops.common.notify.pojo.SmsNotifyPost
import com.tencent.devops.common.notify.utils.ChineseStringUtil
import com.tencent.devops.common.notify.utils.Configuration
import com.tencent.devops.common.notify.utils.NotifyDigestUtils
import com.tencent.devops.model.notify.tables.records.TNotifySmsRecord
import com.tencent.devops.notify.blueking.utils.NotifyService
import com.tencent.devops.notify.blueking.utils.NotifyService.Companion.SMS_URL
import com.tencent.devops.notify.dao.SmsNotifyDao
import com.tencent.devops.notify.model.SmsNotifyMessageWithOperation
import com.tencent.devops.notify.pojo.NotificationResponse
import com.tencent.devops.notify.pojo.NotificationResponseWithPage
import com.tencent.devops.notify.pojo.SmsNotifyMessage
import com.tencent.devops.notify.service.SmsService
import org.slf4j.LoggerFactory
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.beans.factory.annotation.Autowired
import java.util.LinkedList
import java.util.stream.Collectors

@Suppress("ALL")
class SmsServiceImpl @Autowired constructor(
    private val notifyService: NotifyService,
    private val smsNotifyDao: SmsNotifyDao,
    private val streamBridge: StreamBridge,
    private val configuration: Configuration
) : SmsService {

    private val logger = LoggerFactory.getLogger(SmsServiceImpl::class.java)

    override fun sendMqMsg(message: SmsNotifyMessage) {
        message.sendTo(streamBridge)
    }

    override fun sendMessage(smsNotifyMessageWithOperation: SmsNotifyMessageWithOperation) {
        val smsNotifyPosts = generateSmsNotifyPost(smsNotifyMessageWithOperation)
        if (smsNotifyPosts.isEmpty()) {
            logger.warn("List<SmsNotifyPost> is empty after being processed: $smsNotifyMessageWithOperation")
            return
        }

        val retryCount = smsNotifyMessageWithOperation.retryCount
        val batchId = smsNotifyMessageWithOperation.batchId ?: UUIDUtil.generate()
        val tofConfs = configuration.getConfigurations(smsNotifyMessageWithOperation.tofSysId)
        for (notifyPost in smsNotifyPosts) {
            val id = smsNotifyMessageWithOperation.id ?: UUIDUtil.generate()
            val result = notifyService.post(
                SMS_URL, notifyPost, tofConfs!!)
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
                    reSendMessage(notifyPost, smsNotifyMessageWithOperation.source, retryCount + 1, id, batchId)
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
        var delayTime = 0
        when (retryCount) {
            1 -> delayTime = 30000
            2 -> delayTime = 120000
            3 -> delayTime = 300000
        }
        if (delayTime > 0) {
            smsNotifyMessageWithOperation.delayMills = delayTime
        }
        smsNotifyMessageWithOperation.sendTo(streamBridge)
    }

    private fun generateSmsNotifyPost(smsNotifyMessage: SmsNotifyMessage): List<SmsNotifyPost> {
        val list = LinkedList<SmsNotifyPost>()
        val bodyList = ChineseStringUtil.split(smsNotifyMessage.body, 220) ?: return list
        for (i in bodyList.indices) {
            val body = if (bodyList.size > 1) {
                String.format("%s(%d/%d)", bodyList[i], i + 1, bodyList.size)
            } else {
                bodyList[i]
            }

            val contentMd5 = NotifyDigestUtils.getMessageContentMD5("", body)
            val receivers = Lists.newArrayList(filterReceivers(
                smsNotifyMessage.getReceivers(), contentMd5, smsNotifyMessage.frequencyLimit)
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
                    sender = smsNotifyMessage.sender
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
            val emailRecords = smsNotifyDao.list(page, pageSize, success, fromSysId, createdTimeSortOrder)
            emailRecords.stream().map(this::parseFromTNotifySmsToResponse)?.collect(Collectors.toList()) ?: listOf()
        }
        return NotificationResponseWithPage(count, page, pageSize, result)
    }

    private fun parseFromTNotifySmsToResponse(
        record: TNotifySmsRecord
    ): NotificationResponse<SmsNotifyMessageWithOperation> {
        val receivers: MutableSet<String> = mutableSetOf()
        if (!record.receivers.isNullOrEmpty()) {
            receivers.addAll(record.receivers.split(";"))
        }

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
            id = record.id,
            success = record.success,
            createdTime = if (record.createdTime == null) null
            else {
                DateTimeUtil.convertLocalDateTimeToTimestamp(record.createdTime)
            },
            updatedTime = if (record.updatedTime == null) null
            else {
                DateTimeUtil.convertLocalDateTimeToTimestamp(record.updatedTime)
            },
            contentMD5 = record.contentMd5,
            notificationMessage = message
        )
    }
}
