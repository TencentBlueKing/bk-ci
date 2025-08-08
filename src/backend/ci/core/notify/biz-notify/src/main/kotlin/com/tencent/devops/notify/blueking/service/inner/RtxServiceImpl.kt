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
import com.tencent.devops.common.notify.pojo.RtxNotifyPost
import com.tencent.devops.common.notify.utils.ChineseStringUtil
import com.tencent.devops.common.notify.utils.Configuration
import com.tencent.devops.common.notify.utils.NotifyDigestUtils
import com.tencent.devops.model.notify.tables.records.TNotifyRtxRecord
import com.tencent.devops.notify.blueking.utils.NotifyService
import com.tencent.devops.notify.blueking.utils.NotifyService.Companion.RTX_URL
import com.tencent.devops.notify.dao.RtxNotifyDao
import com.tencent.devops.notify.model.RtxNotifyMessageWithOperation
import com.tencent.devops.notify.pojo.NotificationResponse
import com.tencent.devops.notify.pojo.NotificationResponseWithPage
import com.tencent.devops.notify.pojo.RtxNotifyMessage
import com.tencent.devops.notify.service.RtxService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.stream.function.StreamBridge
import java.util.LinkedList
import java.util.stream.Collectors

@Suppress("ALL")
class RtxServiceImpl @Autowired constructor(
    private val notifyService: NotifyService,
    private val rtxNotifyDao: RtxNotifyDao,
    private val streamBridge: StreamBridge,
    private val configuration: Configuration
) : RtxService {
    private val logger = LoggerFactory.getLogger(RtxServiceImpl::class.java)

    override fun sendMqMsg(message: RtxNotifyMessage) {
        message.sendTo(streamBridge)
    }

    /**
     * 发送RTX消息
     * @param rtxNotifyMessageWithOperation 消息对象
     */
    override fun sendMessage(rtxNotifyMessageWithOperation: RtxNotifyMessageWithOperation) {
        val rtxNotifyPosts = generateRtxNotifyPost(rtxNotifyMessageWithOperation)
        if (rtxNotifyPosts.isEmpty()) {
            logger.warn("List<RtxNotifyPost> is empty after being processed: $rtxNotifyMessageWithOperation")
            return
        }

        val retryCount = rtxNotifyMessageWithOperation.retryCount
        val batchId = rtxNotifyMessageWithOperation.batchId ?: UUIDUtil.generate()
        val tofConfs = configuration.getConfigurations(rtxNotifyMessageWithOperation.tofSysId)
        for (notifyPost in rtxNotifyPosts) {
            val id = rtxNotifyMessageWithOperation.id ?: UUIDUtil.generate()
            val result = notifyService.post(
                RTX_URL, notifyPost, tofConfs!!)
            if (result.Ret == 0) {
                // 成功
                rtxNotifyDao.insertOrUpdateRtxNotifyRecord(
                    true, rtxNotifyMessageWithOperation.source, batchId, id,
                    retryCount, null, notifyPost.receiver, notifyPost.sender,
                    notifyPost.title, notifyPost.msgInfo, notifyPost.priority.toInt(),
                    notifyPost.contentMd5, notifyPost.frequencyLimit,
                    tofConfs["sys-id"], notifyPost.fromSysId
                )
            } else {
                // 写入失败记录
                rtxNotifyDao.insertOrUpdateRtxNotifyRecord(
                    false, rtxNotifyMessageWithOperation.source, batchId, id,
                    retryCount, result.ErrMsg, notifyPost.receiver, notifyPost.sender,
                    notifyPost.title, notifyPost.msgInfo, notifyPost.priority.toInt(),
                    notifyPost.contentMd5, notifyPost.frequencyLimit,
                    tofConfs["sys-id"], notifyPost.fromSysId)
                if (retryCount < 3) {
                    // 开始重试
                    reSendMessage(notifyPost, rtxNotifyMessageWithOperation.source, retryCount + 1, id, batchId)
                }
            }
        }
    }

    private fun reSendMessage(
        post: RtxNotifyPost,
        notifySource: EnumNotifySource,
        retryCount: Int,
        id: String,
        batchId: String
    ) {
        val rtxNotifyMessageWithOperation = RtxNotifyMessageWithOperation()
        rtxNotifyMessageWithOperation.apply {
            this.batchId = batchId
            this.id = id
            this.retryCount = retryCount
            this.source = notifySource
            this.sender = post.sender
            this.addAllReceivers(Sets.newHashSet(post.receiver.split(",")))
            this.priority = EnumNotifyPriority.parse(post.priority)
            this.body = post.msgInfo
            this.title = post.title
            this.frequencyLimit = post.frequencyLimit
            this.tofSysId = post.tofSysId
            this.fromSysId = post.fromSysId
        }
        var delayTime = 0
        when (retryCount) {
            1 -> delayTime = 30000
            2 -> delayTime = 120000
            3 -> delayTime = 300000
        }
        if (delayTime > 0) {
            rtxNotifyMessageWithOperation.delayMills = delayTime
        }
        rtxNotifyMessageWithOperation.sendTo(streamBridge)
    }

    private fun generateRtxNotifyPost(rtxNotifyMessage: RtxNotifyMessage): List<RtxNotifyPost> {
        val list = LinkedList<RtxNotifyPost>()
        val bodyList = ChineseStringUtil.split(rtxNotifyMessage.body, 960) ?: return list
        for (i in bodyList.indices) {
            val body = if (bodyList.size > 1) {
                String.format("%s(%d/%d)", bodyList[i], i + 1, bodyList.size)
            } else {
                bodyList[i]
            }

            val contentMd5 = NotifyDigestUtils.getMessageContentMD5(rtxNotifyMessage.title, body)
            val receivers = Lists.newArrayList(filterReceivers(
                rtxNotifyMessage.getReceivers(), contentMd5, rtxNotifyMessage.frequencyLimit)
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

                val post = RtxNotifyPost()
                post.apply {
                    receiver = subReceivers.joinToString(",")
                    msgInfo = body
                    title = rtxNotifyMessage.title
                    priority = rtxNotifyMessage.priority.getValue()
                    sender = rtxNotifyMessage.sender
                    this.contentMd5 = contentMd5
                    frequencyLimit = rtxNotifyMessage.frequencyLimit
                    tofSysId = rtxNotifyMessage.tofSysId
                    fromSysId = rtxNotifyMessage.fromSysId
                }

                list.add(post)
            }
        }
        return list
    }

    private fun filterReceivers(receivers: Set<String>, contentMd5: String, frequencyLimit: Int?): Set<String> {
        val filteredReceivers = HashSet(receivers)
        val filteredOutReceivers = HashSet<String>()
        if (frequencyLimit != null && frequencyLimit > 0) {
            val recordedReceivers = rtxNotifyDao.getReceiversByContentMd5AndTime(
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
    ): NotificationResponseWithPage<RtxNotifyMessageWithOperation> {
        val count = rtxNotifyDao.count(success, fromSysId)
        val result: List<NotificationResponse<RtxNotifyMessageWithOperation>> = if (count == 0) {
            listOf()
        } else {
            val records = rtxNotifyDao.list(page, pageSize, success, fromSysId, createdTimeSortOrder)
            records.stream().map(this::parseFromTNotifyRtxToResponse)?.collect(Collectors.toList()) ?: listOf()
        }
        return NotificationResponseWithPage(count, page, pageSize, result)
    }

    private fun parseFromTNotifyRtxToResponse(
        record: TNotifyRtxRecord
    ): NotificationResponse<RtxNotifyMessageWithOperation> {
        val receivers: MutableSet<String> = mutableSetOf()
        if (!record.receivers.isNullOrEmpty()) {
            receivers.addAll(record.receivers.split(";"))
        }

        val message = RtxNotifyMessageWithOperation()
        message.apply {
            frequencyLimit = record.frequencyLimit
            fromSysId = record.fromSysId
            tofSysId = record.tofSysId
            body = record.body
            sender = record.sender
            title = record.title
            priority = EnumNotifyPriority.parse(record.priority.toString())
            source = EnumNotifySource.parseName(record.source)
            addAllReceivers(receivers)
            retryCount = record.retryCount
            lastError = record.lastError
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
