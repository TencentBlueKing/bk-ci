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

import com.tencent.devops.common.api.enums.PlatformEnum
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.experience.dao.ExperiencePublicDao
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
    private val experienceNotifyService: ExperienceNotifyService,
    private val experienceBaseService: ExperienceBaseService,
    private val experiencePublicDao: ExperiencePublicDao
) {
    fun bindDeviceToken(
        userId: String,
        platform: Int,
        token: String
    ): Result<Boolean> {
        // 检查是否该用户有绑定记录
        val userTokenRecord = experiencePushDao.getByUserId(
            dslContext = dslContext,
            userId = userId
        )
        return if (userTokenRecord != null) {
            // 若不为空，则用户有绑定记录，则检查前端传递的token和数据库表中的token是否一致。若不一致，则修改用户的设备token
            checkAndUpdateToken(
                dslContext = dslContext,
                userId = userId,
                token = token,
                platform = PlatformEnum.of(platform)?.name ?: "ANDROID",
                userTokenRecord = userTokenRecord
            )
        } else {
            // 若用户无绑定记录，则直接插入数据库表
            experiencePushDao.createUserToken(
                dslContext = dslContext,
                userId = userId,
                token = token,
                platform = PlatformEnum.of(platform)?.name ?: "ANDROID"
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

    fun subscribe(
        userId: String,
        experienceHashId: String,
        platform: Int,
        projectId: String,
        bundleIdentifier: String
    ): Result<Boolean> {
        val experienceId = HashUtil.decodeIdToLong(experienceHashId)
        // 判断是否为体验组成员
        val isExperienceGroups = experienceBaseService.isExperienceGroups(
            experienceId = experienceId,
            userId = userId,
            platform = PlatformEnum.of(platform)?.name ?: "ANDROID",
            bundleIdentifier = bundleIdentifier,
            projectId = projectId
        )
        // 判断是否公开体验
        val isPublicExperience = experienceBaseService.isPublicExperience(experienceId)
        return when {
            isExperienceGroups -> {
                if (isPublicExperience) {
                    Result("该体验已订阅，不允许重复订阅", false)
                } else {
                    Result("内部体验默认已订阅", false)
                }
            }
            else -> canSubscribe(
                isPublicExperience = isPublicExperience,
                userId = userId,
                experienceId = experienceId,
                platform = PlatformEnum.of(platform)?.name ?: "ANDROID",
                projectId = projectId,
                bundleIdentifier = bundleIdentifier
            )
        }
    }

    fun canSubscribe(
        isPublicExperience: Boolean,
        userId: String,
        experienceId: Long,
        platform: String,
        projectId: String,
        bundleIdentifier: String
    ): Result<Boolean> {
        return when {
            isPublicExperience -> {
                // 检查前端传输数据是否和公开订阅表中数据一致
                checkPublicExperienceParam(experienceId, platform, projectId, bundleIdentifier)
                val subscription =
                    experiencePushDao.getSubscription(dslContext, userId, projectId, bundleIdentifier, platform)
                        .isNotEmpty
                // 查询公开订阅表是否有记录
                if (subscription) {
                    Result("不可重复订阅", false)
                } else {
                    experiencePushDao.subscribe(
                        dslContext = dslContext,
                        userId = userId,
                        projectId = projectId,
                        bundle = bundleIdentifier,
                        platform = platform
                    )
                    Result("订阅体验成功！", true)
                }
            }
            else -> Result("不允许订阅内部体验", false)
        }
    }

    fun unSubscribe(
        userId: String,
        experienceHashId: String,
        platform: Int,
        projectId: String,
        bundleIdentifier: String
    ): Result<Boolean> {
        val experienceId = HashUtil.decodeIdToLong(experienceHashId)
        // 判断是否为体验组成员
        val isExperienceGroups = experienceBaseService.isExperienceGroups(
            experienceId = experienceId,
            userId = userId,
            platform = PlatformEnum.of(platform)?.name ?: "ANDROID",
            bundleIdentifier = bundleIdentifier,
            projectId = projectId
        )
        // 判断是否公开体验
        val isPublicExperience = experienceBaseService.isPublicExperience(experienceId)
        return when {
            isExperienceGroups -> {
                if (isPublicExperience) {
                    Result(
                        "既是公开体验又是内部体验的应用版本无法自行取消订阅。" +
                                "蓝盾App已不再支持同时选中两种体验范围，请尽快更改发布体验版本的配置。", false
                    )
                } else {
                    Result(
                        "内部体验默认为已订阅状态，无法自行取消。如需取消订阅，" +
                                "请联系产品负责人退出内部体验，退出后将不接收订阅信息。", false
                    )
                }
            }
            else -> {
                canUnSubscribe(
                    isPublicExperience = isPublicExperience,
                    userId = userId,
                    experienceId = experienceId,
                    platform = PlatformEnum.of(platform)?.name ?: "ANDROID",
                    projectId = projectId,
                    bundleIdentifier = bundleIdentifier
                )
            }
        }
    }

    fun canUnSubscribe(
        isPublicExperience: Boolean,
        userId: String,
        experienceId: Long,
        platform: String,
        projectId: String,
        bundleIdentifier: String
    ): Result<Boolean> {
        return when {
            isPublicExperience -> {
                // 检查前端传输数据是否和公开订阅表中数据一致
                checkPublicExperienceParam(experienceId, platform, projectId, bundleIdentifier)
                val subscription =
                    experiencePushDao.getSubscription(dslContext, userId, projectId, bundleIdentifier, platform)
                        .isNotEmpty
                // 查询公开订阅表是否有记录
                if (subscription) {
                    experiencePushDao.unSubscribe(
                        dslContext,
                        userId = userId,
                        projectId = projectId,
                        bundle = bundleIdentifier,
                        platform = platform
                    )
                    Result("取消订阅成功", true)
                } else {
                    Result("由于没有订阅该体验，不允许取消体验", true)
                }
            }
            else -> Result("内部体验不可取消订阅", false)
        }
    }

    // 检查前端传输数据是否和公开订阅表中数据一致
    fun checkPublicExperienceParam(
        experienceId: Long,
        platform: String,
        projectId: String,
        bundleIdentifier: String,
    ) {
        val experiencePublic = experiencePublicDao.getByRecordId(
            dslContext = dslContext,
            recordId = experienceId
        )
        if (platform != experiencePublic?.platform) {
            throw ParamBlankException("Invalid platform")
        }
        if (projectId != experiencePublic.projectId) {
            throw ParamBlankException("Invalid projectId")
        }
        if (bundleIdentifier != experiencePublic.bundleIdentifier) {
            throw ParamBlankException("Invalid bundleIdentifier")
        }
    }

    fun pushMessage(appNotifyMessage: AppNotifyMessage): Result<Boolean> {
        val content = appNotifyMessage.body
        val title = appNotifyMessage.title
        val userId = appNotifyMessage.receiver
        val url = appNotifyMessage.url
        checkNotifyMessage(content, title, userId, url)
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
        val message =
            createAppNotifyMessage(messageId, userTokenRecord.token, content, title, platform, userId, url)
        // 发送MQ消息
        experienceNotifyService.sendMqMsg(message)
        return Result(true)
    }

    fun createAppNotifyMessage(
        messageId: Long,
        token: String,
        content: String,
        title: String,
        platform: String,
        userId: String,
        url: String
    ): AppNotifyMessage {
        val appNotifyMessage = AppNotifyMessage()
        appNotifyMessage.messageId = messageId
        appNotifyMessage.token = token
        appNotifyMessage.body = content
        appNotifyMessage.title = title
        appNotifyMessage.platform = platform
        appNotifyMessage.receiver = userId
        appNotifyMessage.url = url
        return appNotifyMessage
    }

    fun checkNotifyMessage(
        body: String,
        title: String,
        receiver: String,
        url: String
    ) {
        if (body.isBlank()) {
            throw InvalidParamException(
                message = "invalid body:$body",
                errorCode = NotifyMessageCode.ERROR_NOTIFY_INVALID_BODY
            )
        }
        if (title.isBlank()) {
            throw InvalidParamException(
                message = "invalid title:$title",
                errorCode = NotifyMessageCode.ERROR_NOTIFY_INVALID_TITLE
            )
        }
        if (receiver.isBlank()) {
            throw InvalidParamException(
                message = "invalid receiver:$receiver",
                errorCode = NotifyMessageCode.ERROR_NOTIFY_INVALID_RECEIVERS
            )
        }
        if (url.isBlank()) {
            throw InvalidParamException(
                message = "invalid url:$url",
                errorCode = NotifyMessageCode.ERROR_NOTIFY_INVALID_NOTIFY_TYPE
            )
        }
    }
}
