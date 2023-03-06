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
package com.tencent.devops.notify.consumer

import com.tencent.devops.common.notify.enums.WeworkReceiverType
import com.tencent.devops.common.notify.enums.WeworkTextType
import com.tencent.devops.notify.model.EmailNotifyMessageWithOperation
import com.tencent.devops.notify.model.RtxNotifyMessageWithOperation
import com.tencent.devops.notify.model.SmsNotifyMessageWithOperation
import com.tencent.devops.notify.model.WechatNotifyMessageWithOperation
import com.tencent.devops.notify.model.WeworkNotifyMessageWithOperation
import com.tencent.devops.notify.pojo.WeworkNotifyTextMessage
import com.tencent.devops.notify.service.EmailService
import com.tencent.devops.notify.service.OrgService
import com.tencent.devops.notify.service.RtxService
import com.tencent.devops.notify.service.SmsService
import com.tencent.devops.notify.service.WechatService
import com.tencent.devops.notify.service.WeworkService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class NotifyMessageConsumer @Autowired constructor(
    private val rtxService: RtxService,
    private val emailService: EmailService,
    private val smsService: SmsService,
    private val wechatService: WechatService,
    private val weworkService: WeworkService,
    private val orgService: OrgService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(NotifyMessageConsumer::class.java)
    }

    fun onReceiveRtxMessage(rtxNotifyMessageWithOperation: RtxNotifyMessageWithOperation) {
        try {
            val parseStaff = orgService.parseStaff(rtxNotifyMessageWithOperation.getReceivers())
            rtxNotifyMessageWithOperation.clearReceivers()
            rtxNotifyMessageWithOperation.addAllReceivers(parseStaff)
            rtxService.sendMessage(rtxNotifyMessageWithOperation)
        } catch (ignored: Exception) {
            logger.warn("Failed process received RTX message", ignored)
        }
    }

    fun onReceiveEmailMessage(emailNotifyMessageWithOperation: EmailNotifyMessageWithOperation) {
        try {
            val parseStaff = orgService.parseStaff(emailNotifyMessageWithOperation.getReceivers())
            val parseBcc = orgService.parseStaff(emailNotifyMessageWithOperation.getBcc())
            val parseCc = orgService.parseStaff(emailNotifyMessageWithOperation.getCc())

            emailNotifyMessageWithOperation.clearReceivers()
            emailNotifyMessageWithOperation.clearBcc()
            emailNotifyMessageWithOperation.clearCc()

            emailNotifyMessageWithOperation.addAllBccs(parseBcc)
            emailNotifyMessageWithOperation.addAllCcs(parseCc)
            emailNotifyMessageWithOperation.addAllReceivers(parseStaff)
            emailService.sendMessage(emailNotifyMessageWithOperation)
        } catch (ignored: Exception) {
            logger.warn("Failed process received Email message", ignored)
        }
    }

    fun onReceiveSmsMessage(smsNotifyMessageWithOperation: SmsNotifyMessageWithOperation) {
        try {
            val parseStaff = orgService.parseStaff(smsNotifyMessageWithOperation.getReceivers())
            smsNotifyMessageWithOperation.clearReceivers()
            smsNotifyMessageWithOperation.addAllReceivers(parseStaff)
            smsService.sendMessage(smsNotifyMessageWithOperation)
        } catch (ignored: Exception) {
            logger.warn("Failed process received SMS message", ignored)
        }
    }

    fun onReceiveWechatMessage(wechatNotifyMessageWithOperation: WechatNotifyMessageWithOperation) {
        try {
            val parseStaff = orgService.parseStaff(wechatNotifyMessageWithOperation.getReceivers())
            wechatNotifyMessageWithOperation.clearReceivers()
            wechatNotifyMessageWithOperation.addAllReceivers(parseStaff)
            wechatService.sendMessage(wechatNotifyMessageWithOperation)
        } catch (ignored: Exception) {
            logger.warn("Failed process received Wechat message", ignored)
        }
    }

    fun onReceiveWeworkMessage(weworkNotifyMessageWithOperation: WeworkNotifyMessageWithOperation) {
        try {
            val parseStaff = orgService.parseStaff(weworkNotifyMessageWithOperation.getReceivers())
            weworkNotifyMessageWithOperation.clearReceivers()
            weworkNotifyMessageWithOperation.addAllReceivers(parseStaff)
            val weworkNotifyTextMessage = WeworkNotifyTextMessage(
                receivers = parseStaff,
                receiverType = WeworkReceiverType.single,
                textType = if (weworkNotifyMessageWithOperation.markdownContent) {
                    WeworkTextType.markdown
                } else {
                    WeworkTextType.text
                },
                message = weworkNotifyMessageWithOperation.body
            )
            weworkService.sendTextMessage(weworkNotifyTextMessage)
        } catch (ignored: Exception) {
            logger.warn("Failed process received Wework message", ignored)
        }
    }
}
