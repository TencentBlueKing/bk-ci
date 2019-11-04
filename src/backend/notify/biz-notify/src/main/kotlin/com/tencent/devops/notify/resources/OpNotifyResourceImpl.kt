/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.tencent.devops.notify.resources

import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.notify.api.op.OpNotifyResource
import com.tencent.devops.notify.service.EmailService
import com.tencent.devops.notify.service.RtxService
import com.tencent.devops.notify.service.SmsService
import com.tencent.devops.notify.service.WechatService
import com.tencent.devops.notify.pojo.BaseMessage
import com.tencent.devops.notify.pojo.EmailNotifyMessage
import com.tencent.devops.notify.pojo.NotificationResponseWithPage
import com.tencent.devops.notify.pojo.RtxNotifyMessage
import com.tencent.devops.notify.pojo.SmsNotifyMessage
import com.tencent.devops.notify.pojo.WechatNotifyMessage
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpNotifyResourceImpl @Autowired constructor(
    private val emailService: EmailService,
    private val rtxService: RtxService,
    private val smsService: SmsService,
    private val wechatService: WechatService
) : OpNotifyResource {

    override fun sendRtxNotify(message: RtxNotifyMessage): Result<Boolean> {
        if (message.title.isEmpty()) {
            throw ParamBlankException("无效的标题")
        }
        if (message.body.isEmpty()) {
            throw ParamBlankException("无效的内容")
        }
        if (message.isReceiversEmpty()) {
            throw ParamBlankException("无效的接收者")
        }
        rtxService.sendMqMsg(message)
        return Result(true)
    }

    override fun sendEmailNotify(message: EmailNotifyMessage): Result<Boolean> {
        if (message.title.isEmpty()) {
            throw ParamBlankException("无效的标题")
        }
        if (message.body.isEmpty()) {
            throw ParamBlankException("无效的内容")
        }
        if (message.isReceiversEmpty()) {
            throw ParamBlankException("无效的接收者")
        }
        emailService.sendMqMsg(message)
        return Result(true)
    }

    override fun sendWechatNotify(message: WechatNotifyMessage): Result<Boolean> {
        if (message.body.isEmpty()) {
            throw ParamBlankException("无效的内容")
        }
        if (message.isReceiversEmpty()) {
            throw ParamBlankException("无效的接收者")
        }
        wechatService.sendMqMsg(message)
        return Result(true)
    }

    override fun sendSmsNotify(message: SmsNotifyMessage): Result<Boolean> {
        if (message.body.isEmpty()) {
            throw ParamBlankException("无效的内容")
        }
        if (message.isReceiversEmpty()) {
            throw ParamBlankException("无效的接收者")
        }
        smsService.sendMqMsg(message)
        return Result(true)
    }

    override fun listNotifications(type: String?, page: Int, pageSize: Int, success: Boolean?, fromSysId: String?, createdTimeSortOrder: String?): Result<NotificationResponseWithPage<BaseMessage>?> {
        if (type.isNullOrEmpty() || !(type == "rtx" || type == "email" || type == "wechat" || type == "sms")) {
            throw InvalidParamException("无效的通知方式参数type")
        }
        when (type) {
            "rtx" -> return Result(rtxService.listByCreatedTime(page, pageSize, success, fromSysId, createdTimeSortOrder))
            "email" -> return Result(emailService.listByCreatedTime(page, pageSize, success, fromSysId, createdTimeSortOrder))
            "wechat" -> return Result(wechatService.listByCreatedTime(page, pageSize, success, fromSysId, createdTimeSortOrder))
            "sms" -> return Result(smsService.listByCreatedTime(page, pageSize, success, fromSysId, createdTimeSortOrder))
        }
        return Result(0, "")
    }
}