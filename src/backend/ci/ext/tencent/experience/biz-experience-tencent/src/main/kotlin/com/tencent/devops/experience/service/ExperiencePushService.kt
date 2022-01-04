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
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.experience.dao.ExperiencePushDao
import com.tencent.devops.experience.pojo.AppNotifyMessage
import com.tencent.devops.experience.pojo.enums.PushStatus
import com.tencent.devops.model.experience.tables.records.TExperiencePushTokenRecord
import com.tencent.devops.notify.constant.NotifyMessageCode
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ExperiencePushService @Autowired constructor(
    private val dslContext: DSLContext,
    private val experiencePushDao: ExperiencePushDao,
    private val experienceNotifyService: ExperienceNotifyService
) {
    fun bindDeviceToken(
        userId: String,
        platform: String,
        token: String
    ): Result<Boolean> {
        // 检查是否该用户有绑定记录
        val userTokenRecord = experiencePushDao.getByUserId(
            dslContext = dslContext,
            userId = userId
        )
        return if (userTokenRecord != null) {
            // 若不为空，则用户有绑定记录，则检查前端传递的token和数据库表中的token是否一致。若不一致，则修改用户的设备token
            checkAndUpdateToken(dslContext, userId, token, platform, userTokenRecord)
        } else {
            // 若用户无绑定记录，则直接插入数据库表
            experiencePushDao.createUserToken(
                dslContext = dslContext,
                userId = userId,
                token = token,
                platform = platform
            )
            Result("用户绑定设备成功！", true)
        }
    }

    fun checkAndUpdateToken(
        dslContext: DSLContext,
        userId: String,
        token: String,
        platform: String,
        userTokenRecord: TExperiencePushTokenRecord
    ): Result<Boolean> {
        // 前端传递的token和数据库表中token进行比较
        return if (token == userTokenRecord.token) {
            Result("请勿重复绑定同台设备！", false)
        } else {
            val isUpdate = experiencePushDao.updateUserToken(
                dslContext = dslContext,
                userId = userId,
                token = token,
                platform = platform
            )
            when {
                isUpdate -> Result("用户修改设备成功！", true)
                else -> Result("用户修改设备失败！", false)
            }
        }
    }

    fun pushMessage(
        userId: String,
        title: String,
        content: String,
        url: String
    ): Result<Boolean> {
        // todo 有可能去掉该代码，因为后续 账号和设备绑定，可以直接通过账号推送消息
        // 通过userId获取用户绑定设备记录
        val userTokenRecord = experiencePushDao.getByUserId(
            dslContext = dslContext,
            userId = userId
        ) ?: return Result("该用户未绑定设备", false)
        val platform = userTokenRecord.platform
        // 创建推送消息记录，此时状态发送中
        val messageId =
            experiencePushDao.createPushHistory(
                dslContext = dslContext,
                status = PushStatus.SENDING.status,
                receivers = userId,
                title = title,
                content = content,
                url = url,
                platform = platform
            )
        val appNotifyMessage =
            createAppNotifyMessage(messageId, userTokenRecord.token, content, title, platform, userId)
        // 发送MQ消息
        sendAppNotify(appNotifyMessage)
        return Result(true)
    }

    fun createAppNotifyMessage(
        messageId: Long,
        token: String,
        content: String,
        title: String,
        platform: String,
        userId: String
    ): AppNotifyMessage {
        val appNotifyMessage = AppNotifyMessage()
        appNotifyMessage.messageId = messageId
        appNotifyMessage.token = token
        appNotifyMessage.body = content
        appNotifyMessage.title = title
        appNotifyMessage.platform = platform
        appNotifyMessage.receiver = userId
        return appNotifyMessage
    }

    fun sendAppNotify(message: AppNotifyMessage) {
        checkNotifyMessage(message)
        experienceNotifyService.sendMqMsg(message)
    }

    fun checkNotifyMessage(message: AppNotifyMessage) {
        if (message.body.isBlank()) {
            throw InvalidParamException(
                message = "invalid body:${message.body}",
                errorCode = NotifyMessageCode.ERROR_NOTIFY_INVALID_BODY,
                params = arrayOf(message.body)
            )
        }
        if (message.title.isBlank()) {
            throw InvalidParamException(
                message = "invalid title:${message.title}",
                errorCode = NotifyMessageCode.ERROR_NOTIFY_INVALID_TITLE,
                params = arrayOf(message.title)
            )
        }
        if (message.token.isBlank()) {
            throw InvalidParamException(
                message = "invalid token:${message.token}",
                errorCode = NotifyMessageCode.ERROR_NOTIFY_INVALID_NOTIFY_TYPE,
                params = arrayOf(message.token)
            )
        }
        if (message.receiver.isBlank()) {
            throw InvalidParamException(
                message = "invalid receiver:${message.receiver}",
                errorCode = NotifyMessageCode.ERROR_NOTIFY_INVALID_RECEIVERS,
                params = arrayOf(message.receiver)
            )
        }
        if (message.platform.isBlank()) {
            throw InvalidParamException(
                message = "invalid receiver:${message.platform}",
                errorCode = NotifyMessageCode.ERROR_NOTIFY_INVALID_NOTIFY_TYPE,
                params = arrayOf(message.platform)
            )
        }
    }
}
