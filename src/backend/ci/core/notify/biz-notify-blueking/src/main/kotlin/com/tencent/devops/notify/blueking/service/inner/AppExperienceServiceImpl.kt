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

import com.tencent.devops.common.client.Client
import com.tencent.devops.notify.EXCHANGE_NOTIFY
import com.tencent.devops.notify.ROUTE_APP
import com.tencent.devops.notify.model.AppExperienceMessageWithOperation
import com.tencent.devops.notify.pojo.AppExperienceMessage
import com.tencent.devops.notify.service.AppExperienceService
import com.tencent.xinge.XingeApp
import com.tencent.xinge.bean.AudienceType
import com.tencent.xinge.bean.Message
import com.tencent.xinge.bean.MessageAndroid
import com.tencent.xinge.bean.MessageType
import com.tencent.xinge.push.app.PushAppRequest
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@Suppress("ALL")
class AppExperienceServiceImpl @Autowired constructor(
    private val rabbitTemplate: RabbitTemplate,
    private val client: Client,
) : AppExperienceService {
    private val logger = LoggerFactory.getLogger(AppExperienceServiceImpl::class.java)
    override fun sendMqMsg(message: AppExperienceMessage) {
        rabbitTemplate.convertAndSend(EXCHANGE_NOTIFY, ROUTE_APP, message)
    }

    override fun sendMessage(appExperienceMessageWithOperation: AppExperienceMessageWithOperation?) {
        if (appExperienceMessageWithOperation == null) {
            logger.warn(
                "appExperienceMessageWithOperation is " +
                        "empty after being processed: $appExperienceMessageWithOperation"
            )
            return
        }
        val isSuccess = sendXinge(appExperienceMessageWithOperation)
        when{
            // 消息发送成功
            //isSuccess -> client.get(AppExperiencePushResource::class).updatePushHistoryStatus(appExperienceMessageWithOperation.messageId,1)
            // else {
            //消息发送失败
            //client.get(AppExperiencePushMessage::class).updatePushHistoryStatus(appExperienceMessageWithOperation.messageId,2)}
        }
    }

    override fun sendXinge(appExperienceMessageWithOperation: AppExperienceMessageWithOperation): Boolean {
        val xingeApp = XingeApp.Builder()
            .appId("1500026197")
            .secretKey("b17e4f31fa705c5a45b0601e64df45c1")
            .build()
        val pushAppRequest = PushAppRequest()
        //单设备推送
        pushAppRequest.audience_type = AudienceType.token
        pushAppRequest.message_type = MessageType.notify
        val message = Message()
        message.title = appExperienceMessageWithOperation.title
        message.content = appExperienceMessageWithOperation.body
        pushAppRequest.message = message
        val messageAndroid = MessageAndroid()
        message.android = messageAndroid
        val tokenList: ArrayList<String?> = ArrayList()
        tokenList.add(appExperienceMessageWithOperation.token)
        pushAppRequest.token_list = tokenList
        return xingeApp.pushApp(pushAppRequest).get("ret_code") == "0"
    }
}
