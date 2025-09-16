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

package com.tencent.devops.dispatch.devcloud.client

import com.tencent.devops.common.environment.agent.utils.SmartProxyUtil
import com.tencent.devops.dispatch.devcloud.service.TrafficControlService
import okhttp3.Headers
import okhttp3.Headers.Companion.toHeaders
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class CommonClientProxy @Autowired constructor(
    private val trafficControlService: TrafficControlService
) : ClientProxy {

    companion object {
        private val logger = LoggerFactory.getLogger(CommonClientProxy::class.java)
    }

    @Value("\${devCloud.appId}")
    override val devCloudAppId: String = ""

    @Value("\${devCloud.token}")
    override val devCloudToken: String = ""

    @Value("\${devCloud.url}")
    override val devCloudUrl: String = ""

    @Value("\${devCloud.smartProxyToken}")
    override val smartProxyToken: String = ""

    @Value("\${devCloud.newUrl:}")
    val newDevCloudUrl: String = ""

    override fun baseRequest(
        userId: String,
        url: String,
        projectId: String,
        pipelineId: String
    ): Request.Builder {
        val selectedUrl = selectUrl(userId, projectId, pipelineId)
        logger.info("Selected URL $selectedUrl$url for user $userId project $projectId pipeline $pipelineId")
        return Request.Builder().url(selectedUrl + url).headers(headers(projectId, pipelineId, userId))
    }

    private fun selectUrl(userId: String, projectId: String, pipelineId: String): String {
        return try {
            if (trafficControlService.shouldUseNewUrl(projectId, pipelineId)) {
                if (newDevCloudUrl.isNotBlank()) {
                    logger.debug("Traffic control decision: using new URL $newDevCloudUrl for user: $userId, " +
                            "project: $projectId, pipeline: $pipelineId")
                    newDevCloudUrl
                } else {
                    logger.warn("New URL not configured, fallback to old URL for user: $userId, project: $projectId, " +
                            "pipeline: $pipelineId")
                    devCloudUrl
                }
            } else {
                logger.debug("Traffic control decision: using old URL $devCloudUrl for user: $userId, " +
                        "project: $projectId, pipeline: $pipelineId")
                devCloudUrl
            }
        } catch (e: Exception) {
            logger.error("URL selection failed, fallback to old URL for user: $userId, project: $projectId, " +
                    "pipeline: $pipelineId", e)
            devCloudUrl
        }
    }

    fun headers(
        projectId: String,
        pipelineId: String,
        userId: String
    ): Headers {
        return SmartProxyUtil.makeHeaders(
            appId = devCloudAppId,
            token = devCloudToken,
            staffName = userId,
            proxyToken = smartProxyToken,
            projectId = projectId,
            pipelineId = pipelineId
        ).toHeaders()
    }
}
