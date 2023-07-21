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

package com.tencent.devops.process.webhook.element

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.atom.BeforeDeleteParam
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils
import com.tencent.devops.process.engine.service.PipelineWebhookService
import com.tencent.devops.process.plugin.ElementBizPlugin
import com.tencent.devops.process.plugin.annotation.ElementBiz
import com.tencent.devops.process.service.scm.ScmProxyService
import org.springframework.beans.factory.annotation.Autowired

@ElementBiz
class CodeGitWebHookTriggerElementBizPlugin @Autowired constructor(
    private val scmProxyService: ScmProxyService,
    private val pipelineWebhookService: PipelineWebhookService
) : ElementBizPlugin<CodeGitWebHookTriggerElement> {
    override fun elementClass(): Class<CodeGitWebHookTriggerElement> {
        return CodeGitWebHookTriggerElement::class.java
    }

    override fun check(element: CodeGitWebHookTriggerElement, appearedCnt: Int) = Unit

    override fun beforeDelete(element: CodeGitWebHookTriggerElement, param: BeforeDeleteParam) {
        if (param.pipelineId.isNotBlank()) {
            pipelineWebhookService.deleteWebhook(
                projectId = param.projectId,
                pipelineId = param.pipelineId,
                taskId = element.id!!,
                userId = param.userId
            )
        }
    }

    override fun afterCreate(
        element: CodeGitWebHookTriggerElement,
        projectId: String,
        pipelineId: String,
        pipelineName: String,
        userId: String,
        channelCode: ChannelCode,
        create: Boolean,
        container: Container
    ) {
        val variable = (container as TriggerContainer).params.associate { param ->
            param.id to param.defaultValue.toString()
        }
        val repositoryConfig = RepositoryConfigUtils.getRepositoryConfig(
            repoHashId = element.repositoryHashId,
            repoName = element.repositoryName,
            repoType = element.repositoryType,
            variable = variable
        )
        val repository = scmProxyService.addGitWebhook(
            projectId = projectId,
            repositoryConfig = repositoryConfig,
            codeEventType = element.eventType
        )
        pipelineWebhookService.save(
            projectId = projectId,
            pipelineId = pipelineId,
            repositoryType = ScmType.CODE_GIT.name,
            repoType = repositoryConfig.repositoryType.name,
            repoHashId = repositoryConfig.repositoryHashId,
            repoName = repositoryConfig.repositoryName,
            projectName = repository.projectName,
            taskId = element.id!!,
            eventSource = repository.gitProjectId?.toString() ?: repository.projectName,
            eventType = element.eventType!!.name
        )
    }
}
