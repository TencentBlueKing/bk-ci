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
import com.tencent.devops.common.notify.pojo.WechatNotifyPost
import com.tencent.devops.common.notify.utils.Configuration
import com.tencent.devops.common.notify.utils.NotifyDigestUtils
import com.tencent.devops.model.notify.tables.records.TNotifyWechatRecord
import com.tencent.devops.notify.blueking.utils.NotifyService
import com.tencent.devops.notify.blueking.utils.NotifyService.Companion.WECHAT_URL
import com.tencent.devops.notify.dao.WechatNotifyDao
import com.tencent.devops.notify.model.WechatNotifyMessageWithOperation
import com.tencent.devops.notify.pojo.NotificationResponse
import com.tencent.devops.notify.pojo.NotificationResponseWithPage
import com.tencent.devops.notify.pojo.WechatNotifyMessage
import com.tencent.devops.notify.service.WechatService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.stream.function.StreamBridge
import java.util.stream.Collectors

@Suppress("ALL")
class WechatServiceImpl @Autowired constructor(
    private val notifyService: NotifyService,
    private val wechatNotifyDao: WechatNotifyDao,
    private val streamBridge: StreamBridge,
    private val configuration: Configuration
) : WechatService {

    private val logger = LoggerFactory.getLogger(WechatServiceImpl::class.java)

    override fun sendMqMsg(message: WechatNotifyMessage) {
        message.sendTo(streamBridge)
    }

    /**
     * 发送短信消息
     * @param wechatNotifyMessageWithOperation 消息对象
     */
    override fun sendMessage(wechatNotifyMessageWithOperation: WechatNotifyMessageWithOperation) {
        val wechatNotifyPost = generateWechatNotifyPost(wechatNotifyMessageWithOperation)
        if (wechatNotifyPost == null) {
            logger.warn("WechatNotifyPost is empty after being processed: $wechatNotifyMessageWithOperation")
            return
        }

        val retryCount = wechatNotifyMessageWithOperation.retryCount
        val id = wechatNotifyMessageWithOperation.id ?: UUIDUtil.generate()
        val tofConfs = configuration.getConfigurations(wechatNotifyMessageWithOperation.tofSysId)
        val result = notifyService.post(WECHAT_URL, wechatNotifyPost, tofConfs!!)
        if (result.Ret == 0) {
            // 成功
            wechatNotifyDao.insertOrUpdateWechatNotifyRecord(
                success = true,
                source = wechatNotifyMessageWithOperation.source,
                id = id,
                retryCount = retryCount,
                lastErrorMessage = null,
                receivers = wechatNotifyPost.receiver,
                sender = wechatNotifyPost.sender,
                body = wechatNotifyPost.msgInfo,
                priority = wechatNotifyPost.priority.toInt(),
                contentMd5 = wechatNotifyPost.contentMd5,
                frequencyLimit = wechatNotifyPost.frequencyLimit,
                tofSysId = tofConfs["sys-id"],
                fromSysId = wechatNotifyPost.fromSysId
            )
        } else {
            // 写入失败记录
            wechatNotifyDao.insertOrUpdateWechatNotifyRecord(
                success = false,
                source = wechatNotifyMessageWithOperation.source,
                id = id,
                retryCount = retryCount,
                lastErrorMessage = result.ErrMsg,
                receivers = wechatNotifyPost.receiver,
                sender = wechatNotifyPost.sender,
                body = wechatNotifyPost.msgInfo,
                priority = wechatNotifyPost.priority.toInt(),
                contentMd5 = wechatNotifyPost.contentMd5,
                frequencyLimit = wechatNotifyPost.frequencyLimit,
                tofSysId = tofConfs["sys-id"],
                fromSysId = wechatNotifyPost.fromSysId
            )
            if (retryCount < 3) {
                // 开始重试
                reSendMessage(wechatNotifyPost, wechatNotifyMessageWithOperation.source, retryCount + 1, id)
            }
        }
    }

    private fun reSendMessage(post: WechatNotifyPost, source: EnumNotifySource, retryCount: Int, id: String) {
        val wechatNotifyMessageWithOperation = WechatNotifyMessageWithOperation()
        wechatNotifyMessageWithOperation.apply {
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
            wechatNotifyMessageWithOperation.delayMills = delayTime
        }
        wechatNotifyMessageWithOperation.sendTo(streamBridge)
    }

    private fun generateWechatNotifyPost(wechatNotifyMessage: WechatNotifyMessage): WechatNotifyPost? {
        val contentMd5 = NotifyDigestUtils.getMessageContentMD5("", wechatNotifyMessage.body)
        val receivers = Lists.newArrayList(filterReceivers(
            wechatNotifyMessage.getReceivers(), contentMd5, wechatNotifyMessage.frequencyLimit)
        )
        if (receivers == null || receivers.isEmpty()) {
            return null
        }

        val post = WechatNotifyPost()
        post.apply {
            receiver = wechatNotifyMessage.getReceivers().joinToString(",")
            msgInfo = wechatNotifyMessage.body
            priority = wechatNotifyMessage.priority.getValue()
            sender = wechatNotifyMessage.sender
            this.contentMd5 = contentMd5
            frequencyLimit = wechatNotifyMessage.frequencyLimit
            tofSysId = wechatNotifyMessage.tofSysId
            fromSysId = wechatNotifyMessage.fromSysId
        }

        return post
    }

    private fun filterReceivers(receivers: Set<String>, contentMd5: String, frequencyLimit: Int): Set<String> {
        val filteredReceivers = HashSet(receivers)
        val filteredOutReceivers = HashSet<String>()
        if (frequencyLimit > 0) {
            val recordedReceivers = wechatNotifyDao.getReceiversByContentMd5AndTime(
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
    ): NotificationResponseWithPage<WechatNotifyMessageWithOperation> {
        val count = wechatNotifyDao.count(success, fromSysId)
        val result: List<NotificationResponse<WechatNotifyMessageWithOperation>> = if (count == 0) {
            listOf()
        } else {
            val emailRecords = wechatNotifyDao.list(page, pageSize, success, fromSysId, createdTimeSortOrder)
            emailRecords.stream().map(this::parseFromTNotifyWechatToResponse)?.collect(Collectors.toList()) ?: listOf()
        }
        return NotificationResponseWithPage(count, page, pageSize, result)
    }

    private fun parseFromTNotifyWechatToResponse(
        record: TNotifyWechatRecord
    ): NotificationResponse<WechatNotifyMessageWithOperation> {
        val receivers: MutableSet<String> = mutableSetOf()
        if (!record.receivers.isNullOrEmpty()) {
            receivers.addAll(record.receivers.split(";"))
        }

        val message = WechatNotifyMessageWithOperation()
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
