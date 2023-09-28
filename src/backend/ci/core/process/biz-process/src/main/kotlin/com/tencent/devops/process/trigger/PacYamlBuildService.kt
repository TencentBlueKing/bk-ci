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
 *
 */

package com.tencent.devops.process.trigger

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.process.engine.dao.PipelineYamlVersionDao
import com.tencent.devops.process.pojo.trigger.PipelineTriggerDetailBuilder
import com.tencent.devops.process.service.webhook.PipelineBuildWebhookService
import com.tencent.devops.process.trigger.actions.BaseAction
import com.tencent.devops.process.webhook.WebhookEventFactory
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PacYamlBuildService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineYamlVersionDao: PipelineYamlVersionDao,
    private val webhookEventFactory: WebhookEventFactory,
    private val pipelineBuildWebhookService: PipelineBuildWebhookService,
    private val pipelineTriggerEventService: PipelineTriggerEventService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PacYamlBuildService::class.java)
    }

    fun start(
        projectId: String,
        action: BaseAction,
        scmType: ScmType
    ) {
        val yamlFile = action.data.context.yamlFile!!
        val repoHashId = action.data.setting.repoHashId
        val pipelineYamlVersion = pipelineYamlVersionDao.get(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = yamlFile.yamlPath,
            blobId = yamlFile.blobId!!
        ) ?: run {
            logger.info("pac yaml build not found pipeline version|$projectId|$repoHashId|$yamlFile")
            return
        }
        val matcher = webhookEventFactory.createScmWebHookMatcher(scmType = scmType, event = action.data.event)
        val builder = PipelineTriggerDetailBuilder()
            .detailId(pipelineTriggerEventService.getDetailId())
            .projectId(projectId)
            .pipelineId(pipelineYamlVersion.pipelineId)
            .eventId(action.data.context.eventId!!)
            .eventSource(repoHashId)
        pipelineBuildWebhookService.webhookTriggerPipelineBuild(
            projectId = projectId,
            pipelineId = pipelineYamlVersion.pipelineId,
            matcher = matcher,
            builder = builder
        )
        pipelineTriggerEventService.saveTriggerEventDetail(triggerDetail = builder.build())
    }
}
