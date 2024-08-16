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

package com.tencent.devops.support.resources.app

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.kafka.KafkaClient
import com.tencent.devops.common.kafka.KafkaTopic.BK_CI_APP_LOGIN_TOPIC
import com.tencent.devops.common.service.Profile
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.support.api.app.AppAppVersionResource
import com.tencent.devops.support.model.app.pojo.AppVersion
import com.tencent.devops.support.services.AppVersionService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class AppAppVersionResourceImpl @Autowired constructor(
    private val appVersionService: AppVersionService,
    private val kafkaClient: KafkaClient,
    private val profile: Profile
) :
    AppAppVersionResource {
    override fun getAllAppVersion(channelType: Byte): Result<List<AppVersion>> {
        return Result(data = appVersionService.getAllAppVersionByChannelType(channelType))
    }

    override fun getLastAppVersion(
        userId: String,
        appVersion: String?,
        organization: String?,
        channelType: Byte
    ): Result<AppVersion?> {
        try {
            val logData = mapOf(
                "version" to (appVersion ?: "1.0.0"),
                "userId" to userId,
                "organization" to (organization ?: "inner"),
                "timestamp" to System.currentTimeMillis(),
                "channelType" to channelType,
                "profile" to profile.getEnv().name
            )
            kafkaClient.send(BK_CI_APP_LOGIN_TOPIC, JsonUtil.toJson(logData))
        } catch (e: Exception) {
            logger.warn("kafka $BK_CI_APP_LOGIN_TOPIC error", e)
        }
        return Result(data = appVersionService.getLastAppVersion(channelType, appVersion ?: "1.0.0"))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AppAppVersionResourceImpl::class.java)
    }
}
