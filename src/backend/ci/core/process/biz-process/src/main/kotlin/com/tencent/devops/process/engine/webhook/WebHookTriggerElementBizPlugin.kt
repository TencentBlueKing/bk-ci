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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.engine.webhook

import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.atom.BeforeDeleteParam
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitGenericWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGithubWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitlabWebHookTriggerElement
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
        create: Boolean
    ) {
    }

    override fun beforeDelete(element: T, param: BeforeDeleteParam) {
        if (param.pipelineId.isNotBlank()) {
            pipelineWebhookService.deleteWebhook(
                pipelineId = param.pipelineId,
                taskId = element.id!!,
                userId = param.userId
            )
        }
    }

    override fun check(element: T, appearedCnt: Int) {
    }
}

@ElementBiz
class CodeGitWebHookTriggerElementBizPlugin constructor(
    private val pipelineWebhookService: PipelineWebhookService
) : WebHookTriggerElementBizPlugin<CodeGitWebHookTriggerElement>(pipelineWebhookService) {

    override fun elementClass(): Class<CodeGitWebHookTriggerElement> {
        return CodeGitWebHookTriggerElement::class.java
    }
}

@ElementBiz
class CodeGithubWebHookTriggerElementBizPlugin constructor(
    private val pipelineWebhookService: PipelineWebhookService
) : WebHookTriggerElementBizPlugin<CodeGithubWebHookTriggerElement>(pipelineWebhookService) {
    override fun elementClass(): Class<CodeGithubWebHookTriggerElement> {
        return CodeGithubWebHookTriggerElement::class.java
    }
}

@ElementBiz
class CodeGitlabWebHookTriggerElementBizPlugin constructor(
    private val pipelineWebhookService: PipelineWebhookService
) : WebHookTriggerElementBizPlugin<CodeGitlabWebHookTriggerElement>(pipelineWebhookService) {
    override fun elementClass(): Class<CodeGitlabWebHookTriggerElement> {
        return CodeGitlabWebHookTriggerElement::class.java
    }
}

@ElementBiz
class CodeSVNWebHookTriggerElementBizPlugin constructor(
    private val pipelineWebhookService: PipelineWebhookService
) : WebHookTriggerElementBizPlugin<CodeSVNWebHookTriggerElement>(pipelineWebhookService) {
    override fun elementClass(): Class<CodeSVNWebHookTriggerElement> {
        return CodeSVNWebHookTriggerElement::class.java
    }
}

@ElementBiz
class CodeTGitWebHookTriggerElementBizPlugin constructor(
    private val pipelineWebhookService: PipelineWebhookService
) : WebHookTriggerElementBizPlugin<CodeTGitWebHookTriggerElement>(pipelineWebhookService) {

    override fun elementClass(): Class<CodeTGitWebHookTriggerElement> {
        return CodeTGitWebHookTriggerElement::class.java
    }
}

@ElementBiz
class CodeGitGenericWebHookTriggerElementBizPlugin constructor(
    private val pipelineWebhookService: PipelineWebhookService
) : WebHookTriggerElementBizPlugin<CodeGitGenericWebHookTriggerElement>(pipelineWebhookService) {
    override fun elementClass(): Class<CodeGitGenericWebHookTriggerElement> {
        return CodeGitGenericWebHookTriggerElement::class.java
    }
}