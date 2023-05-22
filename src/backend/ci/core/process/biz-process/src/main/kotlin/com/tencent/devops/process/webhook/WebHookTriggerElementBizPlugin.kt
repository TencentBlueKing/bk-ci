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

package com.tencent.devops.process.webhook

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.enums.RepositoryTypeNew
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.atom.BeforeDeleteParam
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitGenericWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGithubWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitlabWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeP4WebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeSVNWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeTGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.WebHookTriggerElement
import com.tencent.devops.process.engine.service.PipelineWebhookService
import com.tencent.devops.process.plugin.ElementBizPlugin
import com.tencent.devops.process.plugin.annotation.ElementBiz

abstract class WebHookTriggerElementBizPlugin<T : WebHookTriggerElement> constructor(
    private val pipelineWebhookService: PipelineWebhookService
) : ElementBizPlugin<T> {
    override fun afterCreate(
        element: T,
        projectId: String,
        pipelineId: String,
        pipelineName: String,
        userId: String,
        channelCode: ChannelCode,
        create: Boolean,
        container: Container
    ) = Unit

    override fun beforeDelete(element: T, param: BeforeDeleteParam) {
        if (param.pipelineId.isNotBlank()) {
            pipelineWebhookService.deleteWebhook(
                projectId = param.projectId,
                pipelineId = param.pipelineId,
                taskId = element.id!!,
                userId = param.userId
            )
        }
    }

    override fun check(element: T, appearedCnt: Int) = Unit
}

@ElementBiz
class CodeGitWebHookTriggerElementBizPlugin constructor(
    pipelineWebhookService: PipelineWebhookService
) : WebHookTriggerElementBizPlugin<CodeGitWebHookTriggerElement>(pipelineWebhookService) {

    override fun elementClass(): Class<CodeGitWebHookTriggerElement> {
        return CodeGitWebHookTriggerElement::class.java
    }
}

@ElementBiz
class CodeGithubWebHookTriggerElementBizPlugin constructor(
    pipelineWebhookService: PipelineWebhookService
) : WebHookTriggerElementBizPlugin<CodeGithubWebHookTriggerElement>(pipelineWebhookService) {
    override fun elementClass(): Class<CodeGithubWebHookTriggerElement> {
        return CodeGithubWebHookTriggerElement::class.java
    }
}

@ElementBiz
class CodeGitlabWebHookTriggerElementBizPlugin constructor(
    pipelineWebhookService: PipelineWebhookService
) : WebHookTriggerElementBizPlugin<CodeGitlabWebHookTriggerElement>(pipelineWebhookService) {
    override fun elementClass(): Class<CodeGitlabWebHookTriggerElement> {
        return CodeGitlabWebHookTriggerElement::class.java
    }
}

@ElementBiz
class CodeSVNWebHookTriggerElementBizPlugin constructor(
    pipelineWebhookService: PipelineWebhookService
) : WebHookTriggerElementBizPlugin<CodeSVNWebHookTriggerElement>(pipelineWebhookService) {
    override fun elementClass(): Class<CodeSVNWebHookTriggerElement> {
        return CodeSVNWebHookTriggerElement::class.java
    }
}

@ElementBiz
class CodeTGitWebHookTriggerElementBizPlugin constructor(
    pipelineWebhookService: PipelineWebhookService
) : WebHookTriggerElementBizPlugin<CodeTGitWebHookTriggerElement>(pipelineWebhookService) {

    override fun elementClass(): Class<CodeTGitWebHookTriggerElement> {
        return CodeTGitWebHookTriggerElement::class.java
    }
}

@ElementBiz
class CodeGitGenericWebHookTriggerElementBizPlugin constructor(
    pipelineWebhookService: PipelineWebhookService
) : WebHookTriggerElementBizPlugin<CodeGitGenericWebHookTriggerElement>(pipelineWebhookService) {
    override fun elementClass(): Class<CodeGitGenericWebHookTriggerElement> {
        return CodeGitGenericWebHookTriggerElement::class.java
    }

    override fun check(element: CodeGitGenericWebHookTriggerElement, appearedCnt: Int) {
        with(element.data.input) {
            if (repositoryType == RepositoryTypeNew.URL &&
                credentialId.isNullOrBlank() &&
                token.isNullOrBlank()
            ) {
                throw ErrorCodeException(
                    errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                    params = arrayOf("credentialId")
                )
            }
        }
    }

    override fun afterCreate(
        element: CodeGitGenericWebHookTriggerElement,
        projectId: String,
        pipelineId: String,
        pipelineName: String,
        userId: String,
        channelCode: ChannelCode,
        create: Boolean,
        container: Container
    ) {
        // 只支持codecc才能自定义hookUrl
        if (channelCode != ChannelCode.CODECC && !element.data.input.hookUrl.isNullOrBlank()) {
            element.data.input.hookUrl = null
        }
    }
}

@ElementBiz
class CodeP4WebHookTriggerElementBizPlugin constructor(
    pipelineWebhookService: PipelineWebhookService
) : WebHookTriggerElementBizPlugin<CodeP4WebHookTriggerElement>(pipelineWebhookService) {

    override fun elementClass(): Class<CodeP4WebHookTriggerElement> {
        return CodeP4WebHookTriggerElement::class.java
    }
}
