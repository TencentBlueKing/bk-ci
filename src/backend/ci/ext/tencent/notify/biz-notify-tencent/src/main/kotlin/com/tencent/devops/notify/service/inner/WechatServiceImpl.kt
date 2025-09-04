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

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.notify.enums.EnumNotifyPriority
import com.tencent.devops.common.notify.enums.EnumNotifySource
import com.tencent.devops.model.notify.tables.records.TNotifyWechatRecord
import com.tencent.devops.notify.dao.WechatNotifyDao
import com.tencent.devops.notify.model.WechatNotifyMessageWithOperation
import com.tencent.devops.notify.pojo.NotificationResponse
import com.tencent.devops.notify.pojo.NotificationResponseWithPage
import com.tencent.devops.notify.pojo.WechatNotifyMessage
import com.tencent.devops.notify.service.WechatService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.util.stream.Collectors

@Suppress("NestedBlockDepth")
@Primary
@Service
class WechatServiceImpl @Autowired constructor(
    private val wechatNotifyDao: WechatNotifyDao
) : WechatService {

    override fun sendMqMsg(message: WechatNotifyMessage) {
        // 微信告警平台已于2021年6月30日下线了
    }

    /**
     * 发送短信消息
     * @param wechatNotifyMessageWithOperation 消息对象
     */
    override fun sendMessage(wechatNotifyMessageWithOperation: WechatNotifyMessageWithOperation) = Unit

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
            val emailRecords = wechatNotifyDao.list(
                page = page,
                pageSize = pageSize,
                success = success,
                fromSysId = fromSysId,
                createdTimeSortOrder = createdTimeSortOrder
            )
            emailRecords.stream().map(this::parseFromTNotifyWechatToResponse)?.collect(Collectors.toList()) ?: listOf()
        }
        return NotificationResponseWithPage(
            count = count,
            page = page,
            pageSize = pageSize,
            data = result
        )
    }

    private fun parseFromTNotifyWechatToResponse(
        record: TNotifyWechatRecord
    ): NotificationResponse<WechatNotifyMessageWithOperation> {
        val receivers: MutableSet<String> = mutableSetOf()
        if (!record.receivers.isNullOrEmpty())
            receivers.addAll(record.receivers.split(";"))

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
