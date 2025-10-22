/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.process.yaml

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.process.engine.dao.PipelineWebhookVersionDao
import com.tencent.devops.process.engine.dao.PipelineYamlVersionDao
import com.tencent.devops.process.service.webhook.PipelineBuildWebhookService
import com.tencent.devops.process.webhook.WebhookEventFactory
import com.tencent.devops.process.yaml.actions.BaseAction
import com.tencent.devops.process.yaml.actions.GitActionCommon
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineYamlBuildService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineYamlVersionDao: PipelineYamlVersionDao,
    private val pipelineWebhookVersionDao: PipelineWebhookVersionDao,
    private val webhookEventFactory: WebhookEventFactory,
    private val pipelineBuildWebhookService: PipelineBuildWebhookService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineYamlBuildService::class.java)
    }

    fun start(
        projectId: String,
        action: BaseAction,
        scmType: ScmType
    ) {
        try {
            val yamlFile = action.data.context.yamlFile!!
            val repoHashId = action.data.setting.repoHashId
            val branch = action.data.eventCommon.branch
            val filePath = yamlFile.yamlPath
            val blobId = yamlFile.blobId!!
            val ref = GitActionCommon.getRealRef(action = action, branch = branch)

            val matcher = webhookEventFactory.createScmWebHookMatcher(scmType = scmType, event = action.data.event)
            val preMatch = matcher.preMatch()
            if (!preMatch.isMatch) {
                logger.info("webhook trigger pre match|${preMatch.reason}")
                return
            }

            /*
            1. 获取当前分支有没有存到文件blob_id对应的版本,如果存在则直接使用当前版本
            2. 如果不存在,则获取当前blob_id对应的最新版本
             */
            val pipelineYamlVersion = pipelineYamlVersionDao.getPipelineYamlVersion(
                dslContext = dslContext,
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath,
                ref = ref,
                blobId = blobId
            ) ?: pipelineYamlVersionDao.getPipelineYamlVersion(
                dslContext = dslContext,
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath,
                blobId = yamlFile.blobId
            ) ?: run {
                logger.info("pac yaml build not found pipeline version|$projectId|$repoHashId|$yamlFile")
                return
            }
            val taskIds = pipelineWebhookVersionDao.getTaskIds(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineYamlVersion.pipelineId,
                version = pipelineYamlVersion.version,
                repoHashId = repoHashId,
                eventType = matcher.getEventType().name
            )
            if (taskIds.isNullOrEmpty()) {
                return
            }
            logger.info(
                "pipeline yaml build|$projectId|$repoHashId|" +
                        "${pipelineYamlVersion.pipelineId}|${pipelineYamlVersion.version}"
            )
            pipelineBuildWebhookService.exactMatchPipelineWebhookBuild(
                projectId = projectId,
                pipelineId = pipelineYamlVersion.pipelineId,
                version = pipelineYamlVersion.version,
                taskIds = taskIds,
                repoHashId = repoHashId,
                matcher = matcher,
                eventId = action.data.context.eventId!!
            )
        } catch (ignored: Throwable) {
            logger.warn("Failed to build pipeline yaml|$projectId|${action.format()}", ignored)
            throw ignored
        }
    }
}
