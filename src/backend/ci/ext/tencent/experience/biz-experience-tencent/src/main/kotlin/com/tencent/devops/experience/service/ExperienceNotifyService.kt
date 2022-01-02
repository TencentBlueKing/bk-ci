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

package com.tencent.devops.experience.service

import com.tencent.devops.experience.dao.ExperiencePushDao
import com.tencent.devops.experience.pojo.AppNotifyMessage
import com.tencent.devops.experience.pojo.AppNotifyMessageWithOperation
import com.tencent.devops.experience.pojo.enums.PushStatus
import com.tencent.devops.notify.EXCHANGE_NOTIFY
import com.tencent.devops.notify.QUEUE_NOTIFY_APP
import com.tencent.devops.notify.ROUTE_APP
import com.tencent.xinge.XingeApp
import com.tencent.xinge.bean.AudienceType
import com.tencent.xinge.bean.Message
import com.tencent.xinge.bean.MessageAndroid
import com.tencent.xinge.bean.MessageType
import com.tencent.xinge.push.app.PushAppRequest
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.Exchange
import org.springframework.amqp.rabbit.annotation.Queue
import org.springframework.amqp.rabbit.annotation.QueueBinding
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ExperienceNotifyService @Autowired constructor(
    private val dslContext: DSLContext,
    private val experiencePushDao: ExperiencePushDao,
    private val rabbitTemplate: RabbitTemplate,
) {
    private val logger = LoggerFactory.getLogger(ExperienceNotifyService::class.java)
    fun sendMqMsg(message: AppNotifyMessage) {
        rabbitTemplate.convertAndSend(EXCHANGE_NOTIFY, ROUTE_APP, message)
    }

    @RabbitListener(
        containerFactory = "rabbitListenerContainerFactory",
        bindings = [
            QueueBinding(
                key = [ROUTE_APP],
                value = Queue(value = QUEUE_NOTIFY_APP, durable = "true"),
                exchange = Exchange(value = EXCHANGE_NOTIFY, durable = "true", delayed = "true", type = "topic")
            )]
    )
    fun onReceiveAppNotifyMessage(AppNotifyMessageWithOperation: AppNotifyMessageWithOperation) {
        try {
            sendMessage(AppNotifyMessageWithOperation)
        } catch (ignored: Exception) {
            logger.warn("Failed process received Wework message", ignored)
        }
    }

    fun sendMessage(appNotifyMessageWithOperation: AppNotifyMessageWithOperation?): Boolean {
        if (appNotifyMessageWithOperation == null) {
            logger.warn(
                "appNotifyMessageWithOperation is " +
                        "empty after being processed: $appNotifyMessageWithOperation"
            )
            return false
        }
        val isSuccess = sendXinge(appNotifyMessageWithOperation)
        when {
            isSuccess -> experiencePushDao.updatePushHistoryStatus(
                dslContext = dslContext,
                id = appNotifyMessageWithOperation.messageId,
                status = PushStatus.SUCCESS.status
            )
            else -> experiencePushDao.updatePushHistoryStatus(
                dslContext = dslContext,
                id = appNotifyMessageWithOperation.messageId,
                status = PushStatus.FAILURE.status
                )
        }
        return isSuccess
    }

    fun sendXinge(AppNotifyMessageWithOperation: AppNotifyMessageWithOperation): Boolean {
        val xingeApp = XingeApp.Builder()
            // todo 一定要把secretKey、appId这些敏感信息放在配置文件中！一定不要发布git
            .appId("appId")
            .secretKey("secretKey")
            .domainUrl("https://api.tpns.tencent.com/")
            .build()
        logger.info("AppNotifyMessageWithOperation.token:  $AppNotifyMessageWithOperation")
        val pushAppRequest = PushAppRequest()
        // 单设备推送
        pushAppRequest.audience_type = AudienceType.token
        pushAppRequest.message_type = MessageType.notify
        val message = Message()
        message.title = AppNotifyMessageWithOperation.title
        message.content = AppNotifyMessageWithOperation.body
        pushAppRequest.message = message
        val messageAndroid = MessageAndroid()
        message.android = messageAndroid
        val tokenList: ArrayList<String?> = ArrayList()
        tokenList.add(AppNotifyMessageWithOperation.token)
        logger.info("tokenList.token:  $tokenList")
        pushAppRequest.token_list = tokenList
        val ret = xingeApp.pushApp(pushAppRequest)
        logger.info("ret_code:  ${ret.get("ret_code")} ,err_msg:  ${ret.get("err_msg")}")
        return xingeApp.pushApp(pushAppRequest).get("ret_code") == "0"
    }
}
