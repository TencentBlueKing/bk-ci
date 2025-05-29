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

package com.tencent.devops.process.trigger.scm

import com.tencent.devops.process.engine.service.PipelineWebhookService
import com.tencent.devops.process.engine.utils.PipelineUtils
import com.tencent.devops.process.pojo.webhook.WebhookTriggerPipeline
import com.tencent.devops.process.yaml.PipelineYamlService
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.scm.api.pojo.webhook.Webhook
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 流水线webhook事件监听者
 */
@Service
class PipelineWebHookEventListener @Autowired constructor(
    private val pipelineWebhookService: PipelineWebhookService,
    private val pipelineYamlService: PipelineYamlService,
    private val webhookTriggerBuildService: WebhookTriggerBuildService,
    private val webhookGrayService: WebhookGrayService
) : WebHookEventListener {

    override fun onEvent(eventId: Long, repository: Repository, webhook: Webhook, replayPipelineId: String?) {
        // 不是灰度仓库,不执行新逻辑
        if (!webhookGrayService.isGrayRepo(scmCode = repository.scmCode, repository.projectName)) {
            return
        }
        // 传统流水线需要执行预匹配逻辑
        if (webhook.skipCi()) {
            logger.info("skip this webhook request|scmCode:${repository.scmCode}")
            return
        }
        val triggerPipelines = if (replayPipelineId != null) {
            // 如果不是流水线ID,说明是重放失败的yaml文件,在pac监听器处理
            if (PipelineUtils.isPipelineId(replayPipelineId)) {
                listOf(WebhookTriggerPipeline(repository.projectId!!, replayPipelineId))
            } else {
                return
            }
        } else {
            pipelineWebhookService.listTriggerPipeline(
                projectId = repository.projectId!!,
                repositoryHashId = repository.repoHashId!!,
                eventType = webhook.eventType
            )
        }

        triggerPipelines.forEach { (projectId, pipelineId, version) ->
            // 流水线开启PAC,并且代码库开启PAC,在pac监听器处理
            pipelineYamlService.getPipelineYamlInfo(projectId = projectId, pipelineId = pipelineId)?.let {
                if (it.repoHashId == repository.repoHashId) {
                    logger.info(
                        "handling in pac webhook listener|" +
                                "projectId:$projectId|pipelineId:$pipelineId|repoHashId:${it.repoHashId}"
                    )
                    return@forEach
                }
            }
            webhookTriggerBuildService.trigger(
                projectId = projectId,
                pipelineId = pipelineId,
                version = version,
                eventId = eventId,
                repository = repository,
                webhook = webhook
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineWebHookEventListener::class.java)
    }
}
