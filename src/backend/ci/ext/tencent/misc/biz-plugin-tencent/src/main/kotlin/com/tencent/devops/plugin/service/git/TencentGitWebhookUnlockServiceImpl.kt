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

package com.tencent.devops.plugin.service.git

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_MR_ID
import com.tencent.devops.plugin.api.pojo.GitWebhookUnlockEvent
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_REPO_NAME
import com.tencent.devops.scm.api.ServiceGitResource
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_MANUAL_UNLOCK
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class TencentGitWebhookUnlockServiceImpl @Autowired constructor(
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val client: Client
) : GitWebhookUnlockService {

    companion object {
        private val logger = LoggerFactory.getLogger(TencentGitWebhookUnlockServiceImpl::class.java)
    }

    override fun addUnlockHookLockEvent(projectId: String, variables: Map<String, String>) {
        val manualUnlock = variables[BK_REPO_GIT_MANUAL_UNLOCK]?.toBoolean() ?: false
        val mrId = variables[PIPELINE_WEBHOOK_MR_ID]?.toLong()
        val repoName = variables[PIPELINE_REPO_NAME]
        logger.info(
            "Add git webhook unlock event|projectId:$projectId|manualUnlock:$manualUnlock|mrId:$mrId|repoName:$repoName"
        )
        if (mrId == null || !manualUnlock || repoName == null) {
            return
        }
        addUnlockHookLockEvent(
            GitWebhookUnlockEvent(
                projectId = projectId,
                repoName = repoName,
                mrId = mrId
            )
        )
    }

    override fun consumeUnlockHookLock(event: GitWebhookUnlockEvent) {
        logger.info("Consume git webhook unlock event ($event)")
        try {
            with(event) {
                retryTime--
                client.getScm(ServiceGitResource::class).unLockHookLock(
                    projectId = event.projectId,
                    repoName = repoName,
                    mrId = mrId
                )
            }
        } catch (t: Throwable) {
            logger.error("Consume git commit check fail. $event", t)
            when (event.retryTime) {
                2 -> addUnlockHookLockEvent(event, 5)
                1 -> addUnlockHookLockEvent(event, 10)
                0 -> addUnlockHookLockEvent(event, 30)
                else -> {
                    logger.error("Consume git webhook unlock retry fail")
                }
            }
        }
    }

    fun addUnlockHookLockEvent(event: GitWebhookUnlockEvent, delay: Int = 0) {
        logger.info("Add git webhook unlock event: $event")
        event.delayMills = delay * 1000
        pipelineEventDispatcher.dispatch(event)
    }
}
