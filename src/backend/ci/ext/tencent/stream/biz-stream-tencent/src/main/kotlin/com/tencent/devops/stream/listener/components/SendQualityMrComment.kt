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

package com.tencent.devops.stream.listener.components

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.webhook.enums.code.tgit.TGitObjectKind
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitMergeRequestEvent
import com.tencent.devops.scm.pojo.MrCommentBody
import com.tencent.devops.stream.listener.BuildEvent
import com.tencent.devops.stream.listener.StreamBuildListenerContextV2
import com.tencent.devops.stream.v2.service.StreamGitTokenService
import com.tencent.devops.stream.v2.service.StreamQualityService
import com.tencent.devops.stream.v2.service.StreamScmService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class SendQualityMrComment @Autowired constructor(
    private val client: Client,
    private val objectMapper: ObjectMapper,
    private val tokenService: StreamGitTokenService,
    private val streamScmService: StreamScmService,
    private val streamQualityService: StreamQualityService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(SendQualityMrComment::class.java)
    }

    fun sendMrComment(context: StreamBuildListenerContextV2, ruleIds: List<String>) {
        if (!context.checkSend()) {
            return
        }
        with(context) {
            val token = tokenService.getToken(requestEvent.gitProjectId)

            ruleIds.forEach { ruleId ->
                val reportData = streamQualityService.getQualityGitMrResult(
                    client = client,
                    gitProjectId = requestEvent.gitProjectId,
                    pipelineName = pipeline.displayName,
                    event = BuildEvent(
                        projectId = buildEvent.projectId,
                        pipelineId = pipeline.pipelineId,
                        userId = buildEvent.userId,
                        buildId = buildEvent.buildId,
                        status = buildEvent.status,
                        startTime = null
                    ),
                    ruleIds = listOf(ruleId)
                )
                if (reportData.first.isEmpty() || reportData.second.isEmpty()) {
                    logger.warn("qualityCheckListener ${buildEvent.buildId} reportData is null $reportData")
                    return
                }

                val gitEvent = try {
                    objectMapper.readValue<GitEvent>(requestEvent.event) as GitMergeRequestEvent
                } catch (e: Throwable) {
                    logger.error("qualityCheckListener get mergeId error ${e.message}")
                    return
                }

                streamScmService.addMrComment(
                    token = token,
                    gitProjectId = requestEvent.gitProjectId.toString(),
                    mrId = gitEvent.object_attributes.id,
                    mrBody = MrCommentBody(reportData)
                )
            }
        }
    }

    private fun StreamBuildListenerContextV2.checkSend(): Boolean {
        with(this) {
            if (!streamSetting.enableMrComment) {
                return false
            }
            if (requestEvent.objectKind != TGitObjectKind.MERGE_REQUEST.value) {
                return false
            }
        }
        return true
    }
}
