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
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.experience.constant.ExperienceMessageCode.BK_CANNOT_BE_CANCELLED_BY_ITSELF
import com.tencent.devops.experience.constant.ExperienceMessageCode.BK_EXPERIENCE_IS_SUBSCRIBED
import com.tencent.devops.experience.constant.ExperienceMessageCode.BK_INTERNAL_EXPERIENCE_CANNOT_UNSUBSCRIBED
import com.tencent.devops.experience.constant.ExperienceMessageCode.BK_INTERNAL_EXPERIENCE_SUBSCRIBED_DEFAULT
import com.tencent.devops.experience.constant.ExperienceMessageCode.BK_NOT_ALLOWED_TO_CANCEL_THE_EXPERIENCE
import com.tencent.devops.experience.constant.ExperienceMessageCode.BK_NOT_REPEATEDLY_BIND
import com.tencent.devops.experience.constant.ExperienceMessageCode.BK_PLATFORM_IS_INCONSISTENT
import com.tencent.devops.experience.constant.ExperienceMessageCode.BK_PLEASE_CHANGE_CONFIGURATION
import com.tencent.devops.experience.constant.ExperienceMessageCode.BK_SUBSCRIPTION_EXPERIENCE_NOT_ALLOWED
import com.tencent.devops.experience.constant.ExperienceMessageCode.BK_SUBSCRIPTION_EXPERIENCE_SUCCESSFUL
import com.tencent.devops.experience.constant.ExperienceMessageCode.BK_UNSUBSCRIBED_SUCCESSFULLY
import com.tencent.devops.experience.constant.ExperienceMessageCode.BK_USER_BOUND_DEVICE_SUCCESSFULLY
import com.tencent.devops.experience.constant.ExperienceMessageCode.BK_USER_FAILED_TO_MODIFY_DEVICE
import com.tencent.devops.experience.constant.ExperienceMessageCode.BK_USER_MODIFIED_DEVICE_SUCCESSFULLY
import com.tencent.devops.experience.constant.ExperienceMessageCode.BK_USER_NOT_BOUND_DEVICE
import com.tencent.devops.experience.dao.ExperiencePublicDao
import com.tencent.devops.experience.dao.ExperiencePushHistoryDao
import com.tencent.devops.experience.dao.ExperiencePushSubscribeDao
import com.tencent.devops.experience.dao.ExperiencePushTokenDao
import com.tencent.devops.experience.pojo.AppNotifyMessage
import com.tencent.devops.experience.pojo.enums.PushStatus
import com.tencent.devops.model.experience.tables.records.TExperiencePublicRecord
import com.tencent.devops.model.experience.tables.records.TExperiencePushTokenRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service

@Service
class ExperiencePushService @Autowired constructor(
    private val dslContext: DSLContext,
    private val experiencePushTokenDao: ExperiencePushTokenDao,
    private val experienceNotifyService: ExperienceNotifyService,
    private val experienceBaseService: ExperienceBaseService,
    private val experiencePublicDao: ExperiencePublicDao,
    private val experiencePushHistoryDao: ExperiencePushHistoryDao,
    private val experiencePushSubscribeDao: ExperiencePushSubscribeDao
) {
    fun bindDeviceToken(
        userId: String,
        platform: Int,
        token: String
    ): Result<Boolean> {
        val userTokenGetByUserId = experiencePushTokenDao.getByUserId(
            dslContext = dslContext,
            userId = userId
        )
        val userTokenGetByToken = experiencePushTokenDao.getByToken(
            dslContext = dslContext,
            token = token
        )
        if (userTokenGetByUserId != null) {
            return checkAndUpdateUserToken(
                dslContext = dslContext,
                userId = userId,
                token = token,
                platform = PlatformEnum.of(platform)?.name ?: "ANDROID",
                userTokenGetByUserId = userTokenGetByUserId,
                userTokenGetByToken = userTokenGetByToken
            )
        } else {
            if (userTokenGetByToken != null) {
                experiencePushTokenDao.deleteUserToken(
                    dslContext = dslContext,
                    userId = userTokenGetByToken.userId,
                    token = token
                )
            }
            try {
                experiencePushTokenDao.createUserToken(
                    dslContext = dslContext,
                    userId = userId,
                    token = token,
                    platform = PlatformEnum.of(platform)?.name ?: "ANDROID"
                )
            } catch (e: DuplicateKeyException) {
                logger.warn("user token is exist , token:$token")
            }
            return Result(
                MessageUtil.getMessageByLocale(
                messageCode = BK_USER_BOUND_DEVICE_SUCCESSFULLY,
                language = I18nUtil.getLanguage(userId)
            ), true)
        }
    }

    // 检查前端传递的token和数据库表中的token是否一致。若不一致，则修改用户的设备token
    private fun checkAndUpdateUserToken(
        dslContext: DSLContext,
        userId: String,
        token: String,
        platform: String,
        userTokenGetByUserId: TExperiencePushTokenRecord,
        userTokenGetByToken: TExperiencePushTokenRecord?
    ): Result<Boolean> {
        return if (token == userTokenGetByUserId.token) {
            Result(
                MessageUtil.getMessageByLocale(
                    messageCode = BK_NOT_REPEATEDLY_BIND,
                    language = I18nUtil.getLanguage(userId)
                ), false)
        } else {
            if (userTokenGetByToken != null) {
                experiencePushTokenDao.deleteUserToken(
                    dslContext = dslContext,
                    userId = userTokenGetByToken.userId,
                    token = token
                )
            }
            val isUpdate = experiencePushTokenDao.updateUserToken(
                dslContext = dslContext,
                userId = userId,
                token = token,
                platform = platform
            )
            when {
                isUpdate -> Result(
                    MessageUtil.getMessageByLocale(
                        messageCode = BK_USER_MODIFIED_DEVICE_SUCCESSFULLY,
                        language = I18nUtil.getLanguage(userId)
                    ), true)
                else -> Result(
                    MessageUtil.getMessageByLocale(
                        messageCode = BK_USER_FAILED_TO_MODIFY_DEVICE,
                        language = I18nUtil.getLanguage(userId)
                    ), false)
            }
        }
    }

    fun subscribe(
        userId: String,
        experienceHashId: String,
        platform: Int
    ): Result<Boolean> {
        val experienceId = HashUtil.decodeIdToLong(experienceHashId)
        val isExperienceGroups = experienceBaseService.isExperienceGroups(
            experienceId = experienceId,
            userId = userId
        )
        val publicExperience = experiencePublicDao.getByRecordId(
            dslContext = dslContext,
            recordId = experienceId
        )
        when {
            isExperienceGroups -> {
                if (publicExperience != null) {
                    return Result(
                        MessageUtil.getMessageByLocale(
                            messageCode = BK_EXPERIENCE_IS_SUBSCRIBED,
                            language = I18nUtil.getLanguage(userId)
                        ), false)
                }
                return Result(
                    MessageUtil.getMessageByLocale(
                        messageCode = BK_INTERNAL_EXPERIENCE_SUBSCRIBED_DEFAULT,
                        language = I18nUtil.getLanguage(userId)
                    ), false)
            }
            // 若不在体验组中，进一步查看能否订阅
            else -> {
                return canSubscribe(
                    publicExperience = publicExperience,
                    userId = userId,
                    platform = PlatformEnum.of(platform)?.name ?: "ANDROID"
                )
            }
        }
    }

    private fun canSubscribe(
        publicExperience: TExperiencePublicRecord?,
        userId: String,
        platform: String
    ): Result<Boolean> {
        if (publicExperience == null) {
            return Result(
                MessageUtil.getMessageByLocale(
                    messageCode = BK_SUBSCRIPTION_EXPERIENCE_NOT_ALLOWED,
                    language = I18nUtil.getLanguage(userId)
                ), false)
        }
        // 查询公开订阅表是否有记录
        val subscriptionRecord =
            experiencePushSubscribeDao.getSubscription(
                dslContext = dslContext,
                userId = userId,
                projectId = publicExperience.projectId,
                bundle = publicExperience.bundleIdentifier,
                platform = platform
            )
        if (subscriptionRecord != null) {
            return Result(
                MessageUtil.getMessageByLocale(
                    messageCode = BK_EXPERIENCE_IS_SUBSCRIBED,
                    language = I18nUtil.getLanguage(userId)
                ), false)
        }
        experiencePushSubscribeDao.createSubscription(
            dslContext = dslContext,
            userId = userId,
            projectId = publicExperience.projectId,
            bundle = publicExperience.bundleIdentifier,
            platform = platform
        )
        return Result(
            MessageUtil.getMessageByLocale(
            messageCode = BK_SUBSCRIPTION_EXPERIENCE_SUCCESSFUL,
            language = I18nUtil.getLanguage(userId)
        ), true)
    }

    fun unSubscribe(
        userId: String,
        experienceHashId: String,
        platform: Int
    ): Result<Boolean> {
        val experienceId = HashUtil.decodeIdToLong(experienceHashId)
        val isExperienceGroups = experienceBaseService.isExperienceGroups(
            experienceId = experienceId,
            userId = userId
        )
        val publicExperience = experiencePublicDao.getByRecordId(
            dslContext = dslContext,
            recordId = experienceId
        )
        when {
            isExperienceGroups -> {
                if (publicExperience != null) {
                    return Result(
                        MessageUtil.getMessageByLocale(
                                    messageCode = BK_PLEASE_CHANGE_CONFIGURATION,
                                    language = I18nUtil.getLanguage(userId)
                                ), false
                    )
                }
                return Result(
                    MessageUtil.getMessageByLocale(
                        messageCode = BK_CANNOT_BE_CANCELLED_BY_ITSELF,
                        language = I18nUtil.getLanguage(userId)
                    ), false
                )
            }
            // 若不在体验组中，进一步查看能否取消订阅
            else -> {
                return canUnSubscribe(
                    publicExperience = publicExperience,
                    userId = userId,
                    platform = PlatformEnum.of(platform)?.name ?: "ANDROID"
                )
            }
        }
    }

    private fun canUnSubscribe(
        publicExperience: TExperiencePublicRecord?,
        userId: String,
        platform: String
    ): Result<Boolean> {
        if (publicExperience == null) {
            return Result(
                MessageUtil.getMessageByLocale(
                    messageCode = BK_INTERNAL_EXPERIENCE_CANNOT_UNSUBSCRIBED,
                    language = I18nUtil.getLanguage(userId)
                ), false)
        }
        // 查询公开订阅表是否有记录
        experiencePushSubscribeDao.getSubscription(
            dslContext = dslContext,
            userId = userId,
            projectId = publicExperience.projectId,
            bundle = publicExperience.bundleIdentifier,
            platform = platform
        ) ?: return Result(
            MessageUtil.getMessageByLocale(
                messageCode = BK_NOT_ALLOWED_TO_CANCEL_THE_EXPERIENCE,
                language = I18nUtil.getLanguage(userId)
            ), false)
        experiencePushSubscribeDao.deleteSubscription(
            dslContext,
            userId = userId,
            projectId = publicExperience.projectId,
            bundle = publicExperience.bundleIdentifier,
            platform = platform
        )
        return Result(
            MessageUtil.getMessageByLocale(
                messageCode = BK_UNSUBSCRIBED_SUCCESSFULLY,
                language = I18nUtil.getLanguage(userId)
            ), true)
    }

    fun pushMessage(appNotifyMessage: AppNotifyMessage): Result<Boolean> {
        val content = appNotifyMessage.body
        val title = appNotifyMessage.title
        val userId = appNotifyMessage.receiver
        val url = appNotifyMessage.url
        val userTokenRecord = experiencePushTokenDao.getByUserId(
            dslContext = dslContext,
            userId = userId
        ) ?: return Result(
            MessageUtil.getMessageByLocale(
                messageCode = BK_USER_NOT_BOUND_DEVICE,
                language = I18nUtil.getLanguage(userId)
            ), false)

        val platform = userTokenRecord.platform
        if (platform != appNotifyMessage.platform) {
            return Result(
                MessageUtil.getMessageByLocale(
                    messageCode = BK_PLATFORM_IS_INCONSISTENT,
                    language = I18nUtil.getLanguage(userId)
                ), false)
        }

        // 创建推送消息记录，此时状态发送中
        val messageId =
            experiencePushHistoryDao.createPushHistory(
                dslContext = dslContext,
                status = PushStatus.SENDING.status,
                receivers = userId,
                title = title,
                content = content,
                url = url,
                platform = platform
            )
        val message =
            createAppNotifyMessage(
                messageId = messageId,
                token = userTokenRecord.token,
                content = content,
                title = title,
                platform = platform,
                userId = userId,
                url = url,
                experienceHashId = appNotifyMessage.experienceHashId
            )
        experienceNotifyService.sendMqMsg(message)
        return Result(true)
    }

    private fun createAppNotifyMessage(
        messageId: Long,
        token: String,
        content: String,
        title: String,
        platform: String,
        userId: String,
        url: String,
        experienceHashId: String
    ): AppNotifyMessage {
        val appNotifyMessage = AppNotifyMessage()
        appNotifyMessage.messageId = messageId
        appNotifyMessage.token = token
        appNotifyMessage.body = content
        appNotifyMessage.title = title
        appNotifyMessage.platform = platform
        appNotifyMessage.receiver = userId
        appNotifyMessage.url = url
        appNotifyMessage.experienceHashId = experienceHashId
        return appNotifyMessage
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ExperienceService::class.java)
    }
}
