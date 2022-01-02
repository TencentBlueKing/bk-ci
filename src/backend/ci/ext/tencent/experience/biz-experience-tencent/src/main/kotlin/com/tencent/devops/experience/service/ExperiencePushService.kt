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

import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.experience.dao.ExperiencePushDao
import com.tencent.devops.experience.pojo.AppExperienceMessage
import com.tencent.devops.experience.pojo.AppExperienceMessageWithOperation
import com.tencent.devops.model.experience.tables.records.TExperiencePushTokenRecord
import com.tencent.devops.notify.EXCHANGE_NOTIFY
import com.tencent.devops.notify.QUEUE_NOTIFY_APP
import com.tencent.devops.notify.ROUTE_APP
import com.tencent.devops.notify.constant.NotifyMessageCode
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
class ExperiencePushService @Autowired constructor(
    private val dslContext: DSLContext,
    private val experiencePushDao: ExperiencePushDao,
    private val rabbitTemplate: RabbitTemplate,
) {
    private val logger = LoggerFactory.getLogger(ExperiencePushService::class.java)

    fun bindDeviceToken(userId: String, token: String): Result<Boolean> {
        // 检查是否该用户有绑定记录
        val userTokenRecord = experiencePushDao.getByUserId(
            dslContext = dslContext,
            userId = userId
        )
        val result = if (userTokenRecord != null) {
            // 若用户有绑定记录，则检查前端传递的token和数据库表中的token是否一致。若不一致，则修改用户的设备token
            checkAndUpdateToken(dslContext, userId, token, userTokenRecord)
        } else {
            // 若用户无绑定记录，则直接插入数据库表
            experiencePushDao.createUserToken(dslContext, userId, token)
            Result("用户绑定设备成功！", true)
        }
        return result
    }

    fun checkAndUpdateToken(
        dslContext: DSLContext,
        userId: String,
        token: String,
        userTokenRecord: TExperiencePushTokenRecord
    ): Result<Boolean> {
        val result = if (token == userTokenRecord.token) {
            Result("请勿重复绑定同台设备！", false)
        } else {
            val isUpdate = experiencePushDao.updateUserToken(
                dslContext = dslContext,
                userId = userId,
                token = token
            )
            when {
                isUpdate -> Result("用户修改设备成功！", true)
                else -> Result("用户修改设备失败！", false)
            }
        }
        return result
    }

    fun createPushHistory(
        userId: String,
        title: String,
        content: String,
        url: String,
        platform: String
    ): Result<Boolean> {
        // todo status 魔法数字
        val messageId = experiencePushDao.createPushHistory(dslContext, 1, userId, content, url, platform)
        val token = experiencePushDao.getByUserId(
            dslContext = dslContext,
            userId = userId
        )?.token ?: throw ParamBlankException("Invalid platform")
        // todo 检验token
        val appExperienceMessage = AppExperienceMessage()
        appExperienceMessage.messageId = messageId
        appExperienceMessage.token = token
        appExperienceMessage.body = content
        appExperienceMessage.title = title
        appExperienceMessage.receiver = userId
        sendAppNotify(appExperienceMessage)
        return Result(true)
    }

    fun sendAppNotify(message: AppExperienceMessage) {
        checkAppMessage(message)
        sendMqMsg(message)
    }

    fun checkAppMessage(message: AppExperienceMessage) {
        if (message.body.isBlank()) {
            throw InvalidParamException(
                message = "invalid body:${message.body}",
                errorCode = NotifyMessageCode.ERROR_NOTIFY_INVALID_BODY,
                params = arrayOf(message.body ?: "")
            )
        }
        if (message.title.isBlank()) {
            throw InvalidParamException(
                message = "invalid title:${message.title}",
                errorCode = NotifyMessageCode.ERROR_NOTIFY_INVALID_TITLE,
                params = arrayOf(message.title ?: "")
            )
        }
        // todo messageId 是否需要判断
        if (message.token.isBlank()) {
            throw InvalidParamException(
                message = "invalid body:${message.token}",
                // todo token 这里要code 要怎么写？
                errorCode = NotifyMessageCode.ERROR_NOTIFY_INVALID_BODY,
                params = arrayOf(message.token)
            )
        }
        if (message.receiver.isBlank()) {
            throw InvalidParamException(
                message = "invalid body:${message.receiver}",
                errorCode = NotifyMessageCode.ERROR_NOTIFY_INVALID_RECEIVERS,
                params = arrayOf(message.receiver)
            )
        }
    }

    fun sendMessage(appExperienceMessageWithOperation: AppExperienceMessageWithOperation?): Boolean {
        if (appExperienceMessageWithOperation == null) {
            logger.warn(
                "appExperienceMessageWithOperation is " +
                        "empty after being processed: $appExperienceMessageWithOperation"
            )
            return false
        }
        val isSuccess = sendXinge(appExperienceMessageWithOperation)
        when {
            // todo 魔法数字
            isSuccess -> experiencePushDao.updatePushHistoryStatus(
                dslContext,
                appExperienceMessageWithOperation.messageId,
                1
            )
            else -> experiencePushDao.updatePushHistoryStatus(
                dslContext,
                appExperienceMessageWithOperation.messageId,
                2
            )
        }
        return isSuccess
    }

    fun sendXinge(appExperienceMessageWithOperation: AppExperienceMessageWithOperation): Boolean {
        val xingeApp = XingeApp.Builder()
            .appId("1500026197")
            .secretKey("b17e4f31fa705c5a45b0601e64df45c1")
            .build()
        val pushAppRequest = PushAppRequest()
        // 单设备推送
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

    // todo 单独封装成消息类
    fun sendMqMsg(message: AppExperienceMessage) {
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
    fun onReceiveAppExperienceMessage(appExperienceMessageWithOperation: AppExperienceMessageWithOperation) {
        try {
            // todo 判断成功与否
           sendMessage(appExperienceMessageWithOperation)
        } catch (ignored: Exception) {
            logger.warn("Failed process received Wework message", ignored)
        }

    }
}
