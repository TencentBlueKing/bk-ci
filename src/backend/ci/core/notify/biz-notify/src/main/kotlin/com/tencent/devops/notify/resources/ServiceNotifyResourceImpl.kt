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
package com.tencent.devops.notify.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.notify.enums.WeworkMediaType
import com.tencent.devops.common.notify.enums.WeworkReceiverType
import com.tencent.devops.common.notify.enums.WeworkTextType
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.notify.api.service.ServiceNotifyResource
import com.tencent.devops.notify.model.WeworkNotifyMessageWithOperation
import com.tencent.devops.notify.pojo.EmailNotifyMessage
import com.tencent.devops.notify.pojo.RtxNotifyMessage
import com.tencent.devops.notify.pojo.SmsNotifyMessage
import com.tencent.devops.notify.pojo.WechatNotifyMessage
import com.tencent.devops.notify.pojo.WeworkNotifyMediaMessage
import com.tencent.devops.notify.pojo.WeworkNotifyTextMessage
import com.tencent.devops.notify.pojo.WeworkRobotNotifyMessage
import com.tencent.devops.notify.service.EmailService
import com.tencent.devops.notify.service.SmsService
import com.tencent.devops.notify.service.WechatService
import com.tencent.devops.notify.service.WeworkService
import com.tencent.devops.notify.util.MessageCheckUtil
import org.springframework.beans.factory.annotation.Autowired
import java.io.InputStream

@RestResource
@Suppress("ALL")
class ServiceNotifyResourceImpl @Autowired constructor(
    private val emailService: EmailService,
    private val smsService: SmsService,
    private val wechatService: WechatService,
    private val weworkService: WeworkService
) : ServiceNotifyResource {

    override fun sendRtxNotify(message: RtxNotifyMessage): Result<Boolean> {
        MessageCheckUtil.checkRtxMessage(message)
        val wechatNotifyMessage = WeworkNotifyMessageWithOperation()
        wechatNotifyMessage.addAllReceivers(message.getReceivers())
        wechatNotifyMessage.body = "${message.title}\n\n${message.body}"
        weworkService.sendMqMsg(wechatNotifyMessage)
        return Result(true)
    }

    override fun sendEmailNotify(message: EmailNotifyMessage): Result<Boolean> {
        MessageCheckUtil.checkEmailMessage(message)
        emailService.sendMqMsg(message)
        return Result(true)
    }

    override fun sendWechatNotify(message: WechatNotifyMessage): Result<Boolean> {
        MessageCheckUtil.checkWechatMessage(message)
        wechatService.sendMqMsg(message)
        return Result(true)
    }

    override fun sendSmsNotify(message: SmsNotifyMessage): Result<Boolean> {
        MessageCheckUtil.checkSmsMessage(message)
        smsService.sendMqMsg(message)
        return Result(true)
    }

    override fun sendWeworkMediaNotify(
        receivers: String,
        receiverType: WeworkReceiverType,
        mediaType: WeworkMediaType,
        mediaName: String,
        inputStream: InputStream
    ): Result<Boolean> {
        val weworkNotifyMediaMessage = WeworkNotifyMediaMessage(
            receivers = receivers.split(",|;".toRegex()),
            receiverType = receiverType,
            mediaInputStream = inputStream,
            mediaType = mediaType,
            mediaName = mediaName
        )
        weworkService.sendMediaMessage(weworkNotifyMediaMessage)
        return Result(true)
    }

    override fun sendWeworkTextNotify(
        receivers: String,
        receiverType: WeworkReceiverType,
        textType: WeworkTextType,
        message: String
    ): Result<Boolean> {
        val weworkNotifyTextMessage = WeworkNotifyTextMessage(
            receivers = receivers.split(",|;".toRegex()),
            receiverType = receiverType,
            textType = textType,
            message = message
        )
        weworkService.sendTextMessage(weworkNotifyTextMessage)
        return Result(true)
    }

    override fun sendWeworkRobotNotify(weworkRobotNotifyMessage: WeworkRobotNotifyMessage): Result<Boolean> {
        val weworkNotifyTextMessage = WeworkNotifyTextMessage(
            receivers = weworkRobotNotifyMessage.receivers.split(",|;".toRegex()),
            receiverType = weworkRobotNotifyMessage.receiverType,
            textType = weworkRobotNotifyMessage.textType,
            message = weworkRobotNotifyMessage.message,
            attachments = weworkRobotNotifyMessage.attachments
        )
        return Result(weworkService.sendTextMessage(weworkNotifyTextMessage))
    }
}
