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

package com.tencent.devops.common.webhook.service.code.handler.github

import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.webhook.annotation.CodeWebhookHandler
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GITHUB_WEBHOOK_CREATE_REF_NAME
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GITHUB_WEBHOOK_CREATE_REF_TYPE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GITHUB_WEBHOOK_CREATE_USERNAME
import com.tencent.devops.common.webhook.pojo.code.WebHookParams
import com.tencent.devops.common.webhook.pojo.code.github.GithubCreateEvent
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilter
import com.tencent.devops.common.webhook.service.code.handler.GitHookTriggerHandler
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.scm.utils.code.git.GitUtils

@CodeWebhookHandler
class GithubCreateTriggerHandler : GitHookTriggerHandler<GithubCreateEvent> {
    override fun eventClass(): Class<GithubCreateEvent> {
        return GithubCreateEvent::class.java
    }

    override fun getUrl(event: GithubCreateEvent): String {
        return event.repository.sshUrl
    }

    override fun getUsername(event: GithubCreateEvent): String {
        return event.sender.login
    }

    override fun getRevision(event: GithubCreateEvent): String {
        return ""
    }

    override fun getRepoName(event: GithubCreateEvent): String {
        return GitUtils.getProjectName(event.repository.sshUrl)
    }

    override fun getBranchName(event: GithubCreateEvent): String {
        return org.eclipse.jgit.lib.Repository.shortenRefName(event.ref)
    }

    override fun getEventType(): CodeEventType {
        return CodeEventType.CREATE
    }

    override fun getMessage(event: GithubCreateEvent): String? {
        return ""
    }

    override fun retrieveParams(
        event: GithubCreateEvent,
        projectId: String?,
        repository: Repository?
    ): Map<String, Any> {
        val startParams = mutableMapOf<String, Any>()
        startParams[BK_REPO_GITHUB_WEBHOOK_CREATE_REF_NAME] = event.ref
        startParams[BK_REPO_GITHUB_WEBHOOK_CREATE_REF_TYPE] = event.ref_type
        startParams[BK_REPO_GITHUB_WEBHOOK_CREATE_USERNAME] = event.sender.login
        return startParams
    }

    override fun getEventFilters(
        event: GithubCreateEvent,
        projectId: String,
        pipelineId: String,
        repository: Repository,
        webHookParams: WebHookParams
    ): List<WebhookFilter> {
        return emptyList()
    }
}
