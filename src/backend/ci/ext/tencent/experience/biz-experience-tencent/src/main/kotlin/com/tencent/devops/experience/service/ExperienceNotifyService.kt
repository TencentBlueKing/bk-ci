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

import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.experience.constant.ExperienceConstant
import com.tencent.devops.experience.dao.ExperiencePushHistoryDao
import com.tencent.devops.experience.pojo.AppNotifyMessage
import com.tencent.devops.experience.pojo.AppNotifyMessageWithOperation
import com.tencent.devops.experience.pojo.enums.PushStatus
import com.tencent.devops.notify.EXCHANGE_NOTIFY
import com.tencent.devops.notify.QUEUE_NOTIFY_PUSH
import com.tencent.devops.notify.ROUTE_PUSH
import com.tencent.xinge.XingeApp
import com.tencent.xinge.bean.AudienceType
import com.tencent.xinge.bean.ClickAction
import com.tencent.xinge.bean.Environment
import com.tencent.xinge.bean.Message
import com.tencent.xinge.bean.MessageAndroid
import com.tencent.xinge.bean.MessageIOS
import com.tencent.xinge.bean.MessageType
import com.tencent.xinge.bean.ios.Alert
import com.tencent.xinge.bean.ios.Aps
import com.tencent.xinge.push.app.PushAppRequest
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.Exchange
import org.springframework.amqp.rabbit.annotation.Queue
import org.springframework.amqp.rabbit.annotation.QueueBinding
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class ExperienceNotifyService @Autowired constructor(
    private val dslContext: DSLContext,
    private val experiencePushHistoryDao: ExperiencePushHistoryDao,
    private val rabbitTemplate: RabbitTemplate,
    private val redisOperation: RedisOperation
) {
    private val logger = LoggerFactory.getLogger(ExperienceNotifyService::class.java)

    @Value("\${android.notify.appid:#{null}}")
    private var androidAppId: String? = null

    @Value("\${android.notify.secretkey:#{null}}")
    private var androidSecretKey: String? = null

    @Value("\${ios.notify.appid:#{null}}")
    private var iosAppId: String? = null

    @Value("\${ios.notify.secretkey:#{null}}")
    private var iosSecretKey: String? = null

    @Value("\${app.notify.domainurl:#{null}}")
    private var domainUrl: String? = null

    // 发送MQ
    fun sendMqMsg(message: AppNotifyMessage) {
        rabbitTemplate.convertAndSend(EXCHANGE_NOTIFY, ROUTE_PUSH, message)
    }

    // 消费MQ消息，然后发送信鸽，修改推送信息状态
    @RabbitListener(
        containerFactory = "rabbitListenerContainerFactory",
        bindings = [
            QueueBinding(
                key = [ROUTE_PUSH],
                value = Queue(value = QUEUE_NOTIFY_PUSH, durable = "true"),
                exchange = Exchange(value = EXCHANGE_NOTIFY, durable = "true", delayed = "true", type = "topic")
            )]
    )
    fun onReceiveAppNotifyMessage(appNotifyMessageWithOperation: AppNotifyMessageWithOperation) {
        try {
            sendMessage(appNotifyMessageWithOperation)
        } catch (ignored: Exception) {
            logger.warn("Failed process received Wework message", ignored)
        }
    }

    private fun sendMessage(appNotifyMessageWithOperation: AppNotifyMessageWithOperation?) {
        if (appNotifyMessageWithOperation == null) {
            logger.warn(
                "appNotifyMessageWithOperation is " +
                        "empty after being processed: $appNotifyMessageWithOperation"
            )
            return
        }
        val isSuccess = sendXinge(appNotifyMessageWithOperation)
        when {
            isSuccess -> {
                experiencePushHistoryDao.updatePushHistoryStatus(
                    dslContext = dslContext,
                    id = appNotifyMessageWithOperation.messageId,
                    status = PushStatus.SUCCESS.status
                )
                redisOperation.sadd(
                    ExperienceConstant.redPointKey(
                        appNotifyMessageWithOperation.receiver
                    ),
                    HashUtil.decodeIdToLong(appNotifyMessageWithOperation.experienceHashId).toString()
                )
            }
            else -> experiencePushHistoryDao.updatePushHistoryStatus(
                dslContext = dslContext,
                id = appNotifyMessageWithOperation.messageId,
                status = PushStatus.FAILURE.status
            )
        }
    }

    private fun sendXinge(appNotifyMessageWithOperation: AppNotifyMessageWithOperation): Boolean {
        logger.info("appNotifyMessageWithOperation:  $appNotifyMessageWithOperation")
        val platform = appNotifyMessageWithOperation.platform
        val appId = if (platform == "ANDROID") androidAppId else iosAppId
        val secretKey = if (platform == "ANDROID") androidSecretKey else iosSecretKey
        val xingeApp = XingeApp.Builder()
            .appId(appId)
            .secretKey(secretKey)
            .domainUrl(domainUrl)
            .build()
        // ANDROID 和 IOS 的推送方式不一样
        val pushAppRequest = if (platform == "ANDROID") {
            createAndroidPushAppRequest(appNotifyMessageWithOperation)
        } else {
            createIosPushAppRequest(appNotifyMessageWithOperation)
        }
        val ret = xingeApp.pushApp(pushAppRequest)
        val retCode = ret.get("ret_code")
        logger.info("ret_code:  $retCode ,err_msg:  ${ret.get("err_msg")}")
        // ret_code为0，则表示发送成功
        return retCode == 0
    }

    private fun createAndroidPushAppRequest(
        appNotifyMessageWithOperation: AppNotifyMessageWithOperation
    ): PushAppRequest {
        val pushAppRequest = PushAppRequest()
        // 单设备推送
        pushAppRequest.audience_type = AudienceType.token
        pushAppRequest.message_type = MessageType.notify
        val message = Message()
        message.title = appNotifyMessageWithOperation.title
        message.content = appNotifyMessageWithOperation.body
        pushAppRequest.message = message
        val messageAndroid = MessageAndroid()
        // action_type 动作类型，1，打开activity或app本身；2，打开浏览器；3，打开Intent
        messageAndroid.action = ClickAction()
        messageAndroid.action.action_type = 3
        messageAndroid.action.intent = appNotifyMessageWithOperation.url
        messageAndroid.badgeType = -2
        message.android = messageAndroid
        val tokenList: ArrayList<String?> = ArrayList()
        tokenList.add(appNotifyMessageWithOperation.token)
        pushAppRequest.token_list = tokenList
        return pushAppRequest
    }

    private fun createIosPushAppRequest(
        appNotifyMessageWithOperation: AppNotifyMessageWithOperation
    ): PushAppRequest {
        val pushAppRequest = PushAppRequest()
        pushAppRequest.audience_type = AudienceType.token
        pushAppRequest.environment = Environment.valueOf("product")
        pushAppRequest.message_type = MessageType.notify
        val message = Message()
        message.title = appNotifyMessageWithOperation.title
        message.content = appNotifyMessageWithOperation.body
        val messageIOS = MessageIOS()
        val alert = Alert()
        val aps = Aps()
        aps.category = appNotifyMessageWithOperation.url
        aps.alert = alert
        aps.badge_type = -2
        messageIOS.aps = aps
        message.ios = messageIOS
        pushAppRequest.message = message
        val tokenList = ArrayList<String>()
        tokenList.add(appNotifyMessageWithOperation.token)
        pushAppRequest.token_list = tokenList
        return pushAppRequest
    }
}
