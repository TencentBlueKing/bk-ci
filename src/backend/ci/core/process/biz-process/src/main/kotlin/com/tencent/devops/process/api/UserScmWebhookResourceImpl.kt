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
 */

package com.tencent.devops.process.api

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.user.UserScmWebhookResource
import com.tencent.devops.process.engine.service.PipelineWebhookService
import com.tencent.devops.process.pojo.webhook.PipelineWebhook
import com.tencent.devops.process.pojo.webhook.WebhookEventType
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserScmWebhookResourceImpl @Autowired constructor(
    private val pipelineWebhookService: PipelineWebhookService
) : UserScmWebhookResource {
    override fun getEventType(scmType: String): Result<List<WebhookEventType>> {
        val eventTypeList = when (scmType) {
            ScmType.CODE_GIT.name, ScmType.CODE_TGIT.name ->
                listOf(
                    WebhookEventType(eventType = CodeEventType.PUSH.name, eventTypeName = "Commit Push Hook"),
                    WebhookEventType(eventType = CodeEventType.TAG_PUSH.name, eventTypeName = "Tag Push Hook"),
                    WebhookEventType(
                        eventType = CodeEventType.MERGE_REQUEST.name,
                        eventTypeName = "Merge Request Hook"
                    ),
                    WebhookEventType(
                        eventType = CodeEventType.MERGE_REQUEST_ACCEPT.name,
                        eventTypeName = "Merge Request Accept Hook"
                    )
                )
            ScmType.GITHUB.name ->
                listOf(
                    WebhookEventType(eventType = CodeEventType.PUSH.name, eventTypeName = "Commit Push Hook"),
                    WebhookEventType(eventType = CodeEventType.CREATE.name, eventTypeName = "Create Branch Or Tag"),
                    WebhookEventType(eventType = CodeEventType.PULL_REQUEST.name, eventTypeName = "Pull Request Hook")
                )
            else ->
                listOf(
                    WebhookEventType(eventType = CodeEventType.PUSH.name, eventTypeName = "Commit Push Hook")
                )
        }
        return Result(eventTypeList)
    }

    override fun listScmWebhook(
        userId: String,
        projectId: String,
        pipelineId: String,
        page: Int?,
        pageSize: Int?
    ): Result<List<PipelineWebhook>> {
        return Result(
            pipelineWebhookService.listWebhook(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                page = page,
                pageSize = pageSize
            )
        )
    }
}
